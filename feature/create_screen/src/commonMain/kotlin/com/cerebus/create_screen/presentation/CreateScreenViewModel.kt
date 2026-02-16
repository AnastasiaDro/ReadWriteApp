package com.cerebus.create_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.decks.domain.models.Deck
import com.cerebus.decks.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class CreateScreenViewModel(
    private val deckRepository: DeckRepository,
) : ViewModel() {
    private companion object {
        const val MAX_DECK_NAME_LENGTH = 40
    }

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CreateScreenEffect>()
    val effects: SharedFlow<CreateScreenEffect> = _effects.asSharedFlow()

    init {
        loadDecks()
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
            CreateScreenAction.OnRefreshDecks -> loadDecks()

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
                viewModelScope.launch {
                    _effects.emit(CreateScreenEffect.OpenDeck(action.deckId))
                }
            }

            is CreateScreenAction.OnDeckLongClick -> {
                val deck = _uiState.value.decks.firstOrNull { it.id == action.deckId } ?: return
                _uiState.update {
                    it.copy(
                        deckPendingDelete = deck,
                        isDeleteDialogVisible = true,
                    )
                }
            }

            CreateScreenAction.OnDismissDeleteDialog -> {
                _uiState.update {
                    it.copy(
                        isDeleteDialogVisible = false,
                        deckPendingDelete = null,
                    )
                }
            }

            CreateScreenAction.OnConfirmDeleteDeck -> {
                deleteDeck()
            }

            CreateScreenAction.OnAddCardsClick,
            CreateScreenAction.OnCloseSuccessDialog,
            -> {
                _uiState.update {
                    it.copy(
                        isSuccessDialogVisible = false,
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

    private fun createDeck() {
        val current = _uiState.value
        val normalizedName = current.deckName.trim()

        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(validationError = CreateValidationError.EMPTY_DECK_NAME) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null) }

            val created = deckRepository.addDeck(
                Deck(
                    id = generateDeckId(),
                    name = normalizedName,
                    coverUri = current.coverUri,
                )
            )

            if (created) {
                loadDecks()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCreateDialogVisible = false,
                        isCoverSourceDialogVisible = false,
                        isSuccessDialogVisible = true,
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

    private fun loadDecks() {
        viewModelScope.launch {
            val decks = deckRepository.getAllDecks()
            _uiState.update { it.copy(decks = decks) }
        }
    }

    private fun deleteDeck() {
        val deck = _uiState.value.deckPendingDelete ?: return
        viewModelScope.launch {
            val deleted = deckRepository.deleteDeck(deck.id)
            if (deleted) {
                _uiState.update {
                    it.copy(
                        decks = it.decks.filterNot { item -> item.id == deck.id },
                        isDeleteDialogVisible = false,
                        deckPendingDelete = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDeleteDialogVisible = false,
                        deckPendingDelete = null,
                        validationError = CreateValidationError.DELETE_DECK_FAILED,
                    )
                }
            }
        }
    }

    private fun generateDeckId(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString(16) {
            repeat(16) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }
}
