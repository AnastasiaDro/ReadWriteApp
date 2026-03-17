package com.cerebus.create_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.utils.CustomResult
import com.cerebus.core.utils.UniqueIdGenerator
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateScreenViewModel(
    private val deckRepository: DeckRepository,
    private val studentDeckRepository: StudentDeckRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deckPackageService: DeckPackageService,
) : ViewModel() {
    private companion object {
        const val MAX_DECK_NAME_LENGTH = 40
        const val MAX_DECKS_PER_EXPORT = 5
    }

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CreateScreenEffect>()
    val effects: SharedFlow<CreateScreenEffect> = _effects.asSharedFlow()

    init {
        observeDecks()
    }

    fun onAction(action: CreateScreenAction) {
        when (action) {
            CreateScreenAction.OnCreateDeckClick -> {
                _uiState.update {
                    it.copy(
                        isCreateDialogVisible = true,
                        validationError = null,
                    )
                }
            }
            CreateScreenAction.OnImportDeckClick -> {
                viewModelScope.launch {
                    _effects.emit(CreateScreenEffect.OpenImportDeckPicker)
                }
            }
            is CreateScreenAction.OnImportDeckFilePicked -> importDeckArchive(action.uri)
            CreateScreenAction.OnDismissCreateDialog -> {
                _uiState.update {
                    it.copy(
                        isCreateDialogVisible = false,
                        isCoverSourceDialogVisible = false,
                        validationError = null,
                    )
                }
            }

            is CreateScreenAction.OnDeckNameChanged -> {
                val limitedName = action.value.take(MAX_DECK_NAME_LENGTH)
                _uiState.update { it.copy(deckName = limitedName, validationError = null) }
            }

            is CreateScreenAction.OnCoverUriSelected -> {
                _uiState.update { it.copy(coverUri = action.uri) }
            }

            CreateScreenAction.OnCoverButtonClick -> {
                _uiState.update { it.copy(isCoverSourceDialogVisible = true) }
            }

            CreateScreenAction.OnDismissCoverSourceDialog -> {
                _uiState.update { it.copy(isCoverSourceDialogVisible = false) }
            }

            CreateScreenAction.OnPickFromGalleryClick -> {
                _uiState.update { it.copy(isCoverSourceDialogVisible = false) }
                viewModelScope.launch { _effects.emit(CreateScreenEffect.OpenGallery) }
            }

            CreateScreenAction.OnTakePhotoClick -> {
                _uiState.update { it.copy(isCoverSourceDialogVisible = false) }
                viewModelScope.launch { _effects.emit(CreateScreenEffect.OpenCamera) }
            }

            CreateScreenAction.OnConfirmCreateDeck -> createDeck()

            is CreateScreenAction.OnDeckClick -> {
                if (_uiState.value.selectedDeckIds.isNotEmpty()) {
                    toggleDeckSelection(action.deckId)
                } else {
                    viewModelScope.launch {
                        _effects.emit(CreateScreenEffect.OpenDeck(action.deckId))
                    }
                }
            }

            is CreateScreenAction.OnDeckLongClick -> {
                toggleDeckSelection(action.deckId)
            }

            CreateScreenAction.OnExportSelectedDecksClick -> exportSelectedDecks()
            CreateScreenAction.OnDeleteSelectedDecksClick -> {
                _uiState.update { it.copy(isDeleteSelectedDialogVisible = true) }
            }
            CreateScreenAction.OnDismissDeleteSelectedDialog -> {
                _uiState.update { it.copy(isDeleteSelectedDialogVisible = false) }
            }
            CreateScreenAction.OnConfirmDeleteSelectedDecks -> deleteSelectedDecks()
            CreateScreenAction.OnClearDeckSelection -> {
                _uiState.update {
                    it.copy(
                        selectedDeckIds = emptySet(),
                        isDeleteSelectedDialogVisible = false,
                        validationError = null,
                    )
                }
            }

            CreateScreenAction.OnAddCardsClick,
            CreateScreenAction.OnCloseSuccessDialog,
            -> {
                val createdDeckId = _uiState.value.createdDeckId
                if (action == CreateScreenAction.OnAddCardsClick && createdDeckId.isNotBlank()) {
                    viewModelScope.launch {
                        _effects.emit(
                            CreateScreenEffect.OpenDeck(
                                deckId = createdDeckId,
                                openAddCardDialog = true,
                            )
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        isSuccessDialogVisible = false,
                        createdDeckId = "",
                        createdDeckName = "",
                        createdDeckCoverUri = null,
                        deckName = "",
                        coverUri = null,
                        validationError = null,
                    )
                }
            }
        }
    }

    private fun importDeckArchive(uri: String) {
        if (uri.isBlank()) return
        viewModelScope.launch {
            val studentId = preferencesRepository.getLastActiveStudentId()
            when (
                val result = deckPackageService.importDeck(
                    archiveUri = uri,
                    assignToStudentId = studentId,
                )
            ) {
                is CustomResult.Success -> {
                    // Deck list is observed from repository and updates automatically.
                }
                is CustomResult.Failure -> {
                    _effects.emit(CreateScreenEffect.ShowImportDeckFailed)
                }
            }
        }
    }

    private fun createDeck() {
        val current = _uiState.value
        val normalizedName = current.deckName.trim()

        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(validationError = CreateValidationError.EMPTY_DECK_NAME) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null) }
            val newDeckId = UniqueIdGenerator.randomAlphanumeric(prefix = "deck")

            val created = deckRepository.addDeck(
                Deck(
                    id = newDeckId,
                    name = normalizedName,
                    coverUri = current.coverUri,
                )
            )

            if (created) {
                val activeStudentId = preferencesRepository.getLastActiveStudentId()
                val isAssigned = activeStudentId.isNullOrBlank() ||
                    studentDeckRepository.assignDeckToStudent(activeStudentId, newDeckId)
                if (!isAssigned) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            validationError = CreateValidationError.CREATE_DECK_FAILED,
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCreateDialogVisible = false,
                        isCoverSourceDialogVisible = false,
                        isSuccessDialogVisible = true,
                        createdDeckId = newDeckId,
                        createdDeckName = normalizedName,
                        createdDeckCoverUri = current.coverUri,
                        deckName = "",
                        coverUri = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        validationError = CreateValidationError.CREATE_DECK_FAILED,
                    )
                }
            }
        }
    }

    private fun observeDecks() {
        viewModelScope.launch {
            deckRepository.observeAllDecks().collect { decks ->
                _uiState.update { state ->
                    state.copy(
                        decks = decks,
                        selectedDeckIds = state.selectedDeckIds.intersect(decks.map { it.id }.toSet()),
                    )
                }
            }
        }
    }

    private fun toggleDeckSelection(deckId: String) {
        _uiState.update { state ->
            val selected = state.selectedDeckIds.toMutableSet()
            if (!selected.add(deckId)) {
                selected.remove(deckId)
            }
            state.copy(
                selectedDeckIds = selected,
                validationError = null,
            )
        }
    }

    private fun deleteSelectedDecks() {
        val selectedIds = _uiState.value.selectedDeckIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeletingSelectedDecks = true,
                    validationError = null,
                )
            }

            var deletedCount = 0
            selectedIds.forEach { id ->
                if (deckRepository.deleteDeck(id)) {
                    deletedCount++
                }
            }

            if (deletedCount == selectedIds.size) {
                _uiState.update {
                    it.copy(
                        selectedDeckIds = emptySet(),
                        isDeleteSelectedDialogVisible = false,
                        isDeletingSelectedDecks = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDeleteSelectedDialogVisible = false,
                        isDeletingSelectedDecks = false,
                        validationError = CreateValidationError.DELETE_DECKS_FAILED,
                    )
                }
            }
        }
    }

    private fun exportSelectedDecks() {
        val selectedIds = _uiState.value.selectedDeckIds.toList()
        if (selectedIds.isEmpty() || _uiState.value.isExportingSelectedDecks) return

        if (selectedIds.size > MAX_DECKS_PER_EXPORT) {
            viewModelScope.launch {
                _effects.emit(CreateScreenEffect.ShowExportSelectedDecksLimitExceeded)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingSelectedDecks = true,
                    validationError = null,
                )
            }

            val exportedFiles = mutableListOf<com.cerebus.core.deck_package.domain.service.DeckPackageExportFile>()
            selectedIds.forEach { deckId ->
                when (val result = deckPackageService.exportDeck(deckId)) {
                    is CustomResult.Success -> exportedFiles += result.data
                    is CustomResult.Failure -> {
                        _effects.emit(CreateScreenEffect.ShowExportSelectedDecksFailed)
                        _uiState.update { it.copy(isExportingSelectedDecks = false) }
                        return@launch
                    }
                }
            }

            _effects.emit(CreateScreenEffect.ShareDeckArchives(exportedFiles))
            _uiState.update { it.copy(isExportingSelectedDecks = false) }
        }
    }

}
