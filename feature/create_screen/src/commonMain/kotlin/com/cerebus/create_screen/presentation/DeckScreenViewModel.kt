package com.cerebus.create_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.game_engine.domain.logic.SrsAvailability
import com.cerebus.core.game_engine.domain.logic.SrsSessionCandidate
import com.cerebus.core.game_engine.domain.logic.planSrsSession
import com.cerebus.core.game_engine.domain.logic.toAvailability
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.utils.localStartOfDayMillis
import com.cerebus.core.utils.nowMillis
import com.cerebus.core.utils.UniqueIdGenerator
import com.cerebus.core.utils.CustomResult
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.core.utils.GameLaunchMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeckScreenViewModel(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val deckPackageService: DeckPackageService,
    private val preferencesRepository: PreferencesRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
    private val reviewLogRepository: ReviewLogRepository,
) : ViewModel() {

    private companion object {
        const val MAX_DECK_NAME_LENGTH = 40
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    private val _uiState = MutableStateFlow(DeckUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()
    private val _effects = MutableStateFlow<DeckScreenEffect?>(null)
    val effects: StateFlow<DeckScreenEffect?> = _effects.asStateFlow()
    private var observeDeckJob: Job? = null

    fun onAction(action: DeckScreenAction) {
        when (action) {
            is DeckScreenAction.Initialize -> startObservingDeck(action.deckId)
            DeckScreenAction.OnExportDeckClick -> exportDeck()
            DeckScreenAction.OnEditNameClick -> {
                _uiState.update {
                    it.copy(
                        isEditNameDialogVisible = true,
                        editingName = it.deckName,
                        validationError = null,
                    )
                }
            }

            DeckScreenAction.OnDismissEditNameDialog -> {
                _uiState.update {
                    it.copy(
                        isEditNameDialogVisible = false,
                        editingName = it.deckName,
                        validationError = null,
                    )
                }
            }

            is DeckScreenAction.OnNameChanged -> {
                _uiState.update {
                    it.copy(
                        editingName = action.value.take(MAX_DECK_NAME_LENGTH),
                        validationError = null,
                    )
                }
            }
            DeckScreenAction.OnStartTrainingClick -> {
                val deckId = _uiState.value.deckId
                if (deckId.isNotBlank()) {
                    _effects.value = DeckScreenEffect.OpenGame(
                        deckId = deckId,
                        mode = GameLaunchMode.Plan,
                    )
                }
            }

            DeckScreenAction.OnStartRandomLearnedClick -> {
                val deckId = _uiState.value.deckId
                if (deckId.isNotBlank()) {
                    _effects.value = DeckScreenEffect.OpenGame(
                        deckId = deckId,
                        mode = GameLaunchMode.RandomLearned,
                    )
                }
            }

            DeckScreenAction.OnStartRandomAllClick -> {
                val deckId = _uiState.value.deckId
                if (deckId.isNotBlank()) {
                    _effects.value = DeckScreenEffect.OpenGame(
                        deckId = deckId,
                        mode = GameLaunchMode.RandomAll,
                    )
                }
            }

            DeckScreenAction.OnOpenGalleryClick -> {
                val deckId = _uiState.value.deckId
                if (deckId.isNotBlank()) {
                    _effects.value = DeckScreenEffect.OpenGallery(deckId = deckId)
                }
            }

            DeckScreenAction.OnSaveNameClick -> updateDeckName()
            DeckScreenAction.OnEditCoverClick -> {
                _uiState.update { it.copy(isEditCoverSourceDialogVisible = true) }
            }

            DeckScreenAction.OnDismissEditCoverSourceDialog -> {
                _uiState.update { it.copy(isEditCoverSourceDialogVisible = false) }
            }

            is DeckScreenAction.OnPickImageFromGallery -> openPicker(
                request = DeckPickerRequest.GALLERY,
                target = action.target,
            )
            is DeckScreenAction.OnTakeImagePhoto -> openPicker(
                request = DeckPickerRequest.CAMERA,
                target = action.target,
            )

            DeckScreenAction.OnPickerRequestConsumed -> {
                _uiState.update { it.copy(pendingPickerRequest = null) }
            }

            is DeckScreenAction.OnImagePicked -> onImagePicked(
                uri = action.uri,
                target = action.target,
            )
            DeckScreenAction.OnAddCardClick -> {
                _uiState.update {
                    it.copy(
                        isAddCardDialogVisible = true,
                        cardEditorMode = CardEditorMode.CREATE,
                        editingCardId = null,
                        validationError = null,
                        cardName = "",
                        cardImageUrl = null,
                    )
                }
            }

            DeckScreenAction.OnDismissAddCardDialog -> {
                _uiState.update {
                    it.copy(
                        isAddCardDialogVisible = false,
                        editingCardId = null,
                        isCardCoverSourceDialogVisible = false,
                        isCardSaving = false,
                        validationError = null,
                    )
                }
            }

            is DeckScreenAction.OnCardNameChanged -> {
                _uiState.update {
                    it.copy(
                        cardName = action.value.take(MAX_DECK_NAME_LENGTH),
                        validationError = null,
                    )
                }
            }

            DeckScreenAction.OnCardCoverButtonClick -> {
                _uiState.update { it.copy(isCardCoverSourceDialogVisible = true) }
            }

            DeckScreenAction.OnDismissCardCoverSourceDialog -> {
                _uiState.update { it.copy(isCardCoverSourceDialogVisible = false) }
            }

            DeckScreenAction.OnConfirmAddCard -> saveCard()
            is DeckScreenAction.OnCardLongPress -> toggleCardSelection(action.cardId)
            is DeckScreenAction.OnCardClick -> onCardClick(action.cardId)
            is DeckScreenAction.OnOpenCardEditor -> openCardEditor(action.cardId)
            DeckScreenAction.OnDeleteSelectedCardsClick -> {
                _uiState.update { it.copy(isDeleteSelectedDialogVisible = true) }
            }
            DeckScreenAction.OnDismissDeleteSelectedCardsDialog -> {
                _uiState.update { it.copy(isDeleteSelectedDialogVisible = false) }
            }
            DeckScreenAction.OnConfirmDeleteSelectedCards -> deleteSelectedCards()
            DeckScreenAction.OnClearCardSelection -> {
                _uiState.update {
                    it.copy(
                        selectedCardIds = emptySet(),
                        isDeleteSelectedDialogVisible = false,
                    )
                }
            }
        }
    }

    private fun exportDeck() {
        val deckId = _uiState.value.deckId
        if (deckId.isBlank() || _uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            when (val result = deckPackageService.exportDeck(deckId)) {
                is CustomResult.Success -> {
                    _effects.value = DeckScreenEffect.ShareDeckArchive(
                        filePath = result.data.path,
                        fileName = result.data.fileName,
                    )
                }
                is CustomResult.Failure -> {
                    _effects.value = DeckScreenEffect.ShowExportDeckFailed
                }
            }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun startObservingDeck(deckId: String) {
        if (_uiState.value.deckId == deckId && observeDeckJob?.isActive == true) return

        observeDeckJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                deckId = deckId,
                validationError = null,
            )
        }

        observeDeckJob = viewModelScope.launch {
            val studentId = preferencesRepository.getLastActiveStudentId().orEmpty()
            val progressFlow = if (studentId.isBlank()) {
                flowOf(emptyList())
            } else {
                cardProgressRepository.observeProgress(studentId)
            }
            combine(
                deckRepository.observeDeckById(deckId),
                flashcardRepository.observeFlashcardsByDeckId(deckId),
                progressFlow,
            ) { deck, flashcards, progressList ->
                Triple(deck, flashcards, progressList)
            }.collect { (deck, flashcards, progressList) ->
                _uiState.update { state ->
                    if (deck == null) {
                        state.copy(
                            deckName = "",
                            coverUri = null,
                            flashcards = emptyList(),
                            selectedCardIds = emptySet(),
                            isLoading = false,
                            validationError = DeckValidationError.DECK_NOT_FOUND,
                            srsAvailability = null,
                        )
                    } else {
                        val existingCardIds = flashcards.asSequence().map { it.id }.toSet()
                        val srsAvailability = buildSrsAvailability(
                            studentId = studentId,
                            deckId = deckId,
                            flashcards = flashcards,
                            progressByCardId = progressList.associateBy(CardProgress::cardId),
                        )
                        state.copy(
                            deckName = deck.name,
                            coverUri = deck.coverUri,
                            editingName = if (state.isEditNameDialogVisible) state.editingName else deck.name,
                            flashcards = flashcards,
                            selectedCardIds = state.selectedCardIds.filterTo(mutableSetOf()) { it in existingCardIds },
                            isLoading = false,
                            validationError = if (state.validationError == DeckValidationError.DECK_NOT_FOUND) {
                                null
                            } else {
                                state.validationError
                            },
                            srsAvailability = srsAvailability,
                        )
                    }
                }
            }
        }
    }

    private suspend fun buildSrsAvailability(
        studentId: String,
        deckId: String,
        flashcards: List<Flashcard>,
        progressByCardId: Map<String, CardProgress>,
    ): SrsAvailability? {
        if (studentId.isBlank() || flashcards.isEmpty()) return null

        val prefs = runCatching { studentPrefsRepository.getPrefs(studentId) }.getOrNull() ?: return null
        val guidedHintThreshold = prefs.guidedHintSuccessThreshold.coerceIn(0, 5)
        val dayStartMillis = localStartOfDayMillis()
        val introducedTodayCardIds = reviewLogRepository.getCardIdsFirstReviewedSince(
            studentId = studentId,
            sinceEpochMillis = dayStartMillis,
        )
        val deckCardIds = flashcards.mapTo(mutableSetOf()) { it.id }
        val remainingDailyNewSlots = (
            prefs.maxNewCardsPerDay.coerceAtLeast(0) -
                introducedTodayCardIds.count { it in deckCardIds }
            ).coerceAtLeast(0)
        val candidates = flashcards.map { flashcard ->
            val progress = progressByCardId[flashcard.id]
            SrsSessionCandidate(
                item = flashcard.id,
                deckId = deckId,
                cardId = flashcard.id,
                progressLevel = progress?.level,
                dueAtEpochMillis = progress?.dueAtEpochMillis,
                showHintInitially = shouldShowHintInitially(
                    progress = progress,
                    guidedHintThreshold = guidedHintThreshold,
                ),
            )
        }
        return planSrsSession(
            candidates = candidates,
            introducedTodayCardIds = introducedTodayCardIds,
            reviewLimit = prefs.reviewsPerSession.coerceAtLeast(0),
            newSessionLimit = prefs.newCardsPerSession.coerceAtLeast(0),
            remainingDailyNewLimit = remainingDailyNewSlots,
            nowEpochMillis = nowMillis(),
            dayEndEpochMillis = dayStartMillis + MILLIS_PER_DAY,
        ).toAvailability()
    }

    private fun addCard() {
        val state = _uiState.value
        val normalizedName = state.cardName.trim()
        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(validationError = DeckValidationError.EMPTY_CARD_NAME) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCardSaving = true, validationError = null) }
            val newCard = Flashcard(
                id = UniqueIdGenerator.randomAlphanumeric(prefix = "card"),
                imageUrl = state.cardImageUrl.orEmpty(),
                name = normalizedName,
                deckId = state.deckId,
            )
            val created = flashcardRepository.addFlashcard(newCard)
            if (created) {
                _uiState.update {
                    it.copy(
                        isCardSaving = false,
                        isAddCardDialogVisible = false,
                        editingCardId = null,
                        isCardCoverSourceDialogVisible = false,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCardSaving = false,
                        validationError = DeckValidationError.ADD_CARD_FAILED,
                    )
                }
            }
        }
    }

    private fun updateCard(cardId: String) {
        val state = _uiState.value
        val normalizedName = state.cardName.trim()
        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(validationError = DeckValidationError.EMPTY_CARD_NAME) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCardSaving = true, validationError = null) }
            val updatedCard = Flashcard(
                id = cardId,
                imageUrl = state.cardImageUrl.orEmpty(),
                name = normalizedName,
                deckId = state.deckId,
            )
            val updated = flashcardRepository.updateFlashcard(cardId, updatedCard)
            if (updated) {
                _uiState.update {
                    it.copy(
                        isCardSaving = false,
                        isAddCardDialogVisible = false,
                        editingCardId = null,
                        isCardCoverSourceDialogVisible = false,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCardSaving = false,
                        validationError = DeckValidationError.UPDATE_CARD_FAILED,
                    )
                }
            }
        }
    }

    private fun saveCard() {
        val state = _uiState.value
        when (state.cardEditorMode) {
            CardEditorMode.CREATE -> addCard()
            CardEditorMode.EDIT -> {
                val cardId = state.editingCardId ?: return
                updateCard(cardId)
            }
        }
    }

    private fun onCardClick(cardId: String) {
        val isSelectionMode = _uiState.value.selectedCardIds.isNotEmpty()
        if (isSelectionMode) {
            toggleCardSelection(cardId)
            return
        }

        val deckId = _uiState.value.deckId
        val cardExists = _uiState.value.flashcards.any { it.id == cardId }
        if (deckId.isBlank() || !cardExists) return

        _effects.value = DeckScreenEffect.OpenGallery(
            deckId = deckId,
            cardId = cardId,
        )
    }

    private fun openCardEditor(cardId: String) {
        val card = _uiState.value.flashcards.firstOrNull { it.id == cardId } ?: return
        _uiState.update {
            it.copy(
                isAddCardDialogVisible = true,
                cardEditorMode = CardEditorMode.EDIT,
                editingCardId = card.id,
                cardName = card.name,
                cardImageUrl = card.imageUrl,
                validationError = null,
            )
        }
    }

    private fun toggleCardSelection(cardId: String) {
        _uiState.update { state ->
            val selected = state.selectedCardIds.toMutableSet()
            if (!selected.add(cardId)) {
                selected.remove(cardId)
            }
            state.copy(
                selectedCardIds = selected,
                validationError = null,
            )
        }
    }

    private fun deleteSelectedCards() {
        val current = _uiState.value
        if (current.selectedCardIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeletingSelectedCards = true,
                    validationError = null,
                )
            }

            val selectedIds = _uiState.value.selectedCardIds.toList()
            val deletedIds = mutableSetOf<String>()
            selectedIds.forEach { id ->
                if (flashcardRepository.deleteFlashcard(id)) {
                    deletedIds.add(id)
                }
            }

            _uiState.update { state ->
                val hasFailures = deletedIds.size != selectedIds.size
                val remainingSelection = state.selectedCardIds - deletedIds
                state.copy(
                    selectedCardIds = remainingSelection,
                    isDeletingSelectedCards = false,
                    isDeleteSelectedDialogVisible = false,
                    validationError = if (hasFailures) DeckValidationError.DELETE_CARDS_FAILED else null,
                )
            }
        }
    }

    private fun onImagePicked(
        uri: String,
        target: DeckPickerTarget?,
    ) {
        val resolvedTarget = target ?: _uiState.value.pendingPickerTarget
        when (resolvedTarget) {
            DeckPickerTarget.DECK_COVER -> applyPickedImage(
                uri = uri,
                stateTransform = { state, value ->
                    state.copy(
                        coverUri = value,
                        pendingPickerTarget = null,
                        validationError = null,
                    )
                },
                afterApply = { persistDeckCover(uri) },
            )
            DeckPickerTarget.CARD_IMAGE -> applyPickedImage(
                uri = uri,
                stateTransform = { state, value ->
                    state.copy(
                        cardImageUrl = value,
                        pendingPickerTarget = null,
                        validationError = null,
                    )
                },
            )
            null -> Unit
        }
    }

    private fun updateDeckName() {
        val state = _uiState.value
        val normalizedName = state.editingName.trim()
        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(validationError = DeckValidationError.EMPTY_DECK_NAME) }
            return
        }

        viewModelScope.launch {
            val updated = deckRepository.updateDeckName(state.deckId, normalizedName)
            if (updated) {
                _uiState.update {
                    it.copy(
                        deckName = normalizedName,
                        editingName = normalizedName,
                        isEditNameDialogVisible = false,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update { it.copy(validationError = DeckValidationError.UPDATE_NAME_FAILED) }
            }
        }
    }

    private fun persistDeckCover(uri: String) {
        val state = _uiState.value
        if (state.deckId.isBlank()) {
            _uiState.update { it.copy(validationError = DeckValidationError.UPDATE_COVER_FAILED) }
            return
        }

        viewModelScope.launch {
            val updated = deckRepository.updateDeckCoverUri(state.deckId, uri)
            if (updated) {
            } else {
                _uiState.update { it.copy(validationError = DeckValidationError.UPDATE_COVER_FAILED) }
            }
        }
    }

    private fun applyPickedImage(
        uri: String,
        stateTransform: (DeckUiState, String) -> DeckUiState,
        afterApply: (() -> Unit)? = null,
    ) {
        _uiState.update { current ->
            stateTransform(current, uri)
        }
        afterApply?.invoke()
    }

    private fun openPicker(
        request: DeckPickerRequest,
        target: DeckPickerTarget,
    ) {
        _uiState.update {
            it.copy(
                isEditCoverSourceDialogVisible = false,
                isCardCoverSourceDialogVisible = false,
                pendingPickerRequest = request,
                pendingPickerTarget = target,
            )
        }
    }

    override fun onCleared() {
        observeDeckJob?.cancel()
        super.onCleared()
    }
}

private fun shouldShowHintInitially(
    progress: CardProgress?,
    guidedHintThreshold: Int,
): Boolean {
    if (guidedHintThreshold <= 0) return false
    if (progress == null) return true
    return progress.copySuccessStreak < guidedHintThreshold
}

sealed interface DeckScreenEffect {
    data class OpenGame(
        val deckId: String,
        val mode: GameLaunchMode,
    ) : DeckScreenEffect
    data object ShowExportDeckFailed : DeckScreenEffect
    data class OpenGallery(
        val deckId: String,
        val cardId: String? = null,
    ) : DeckScreenEffect
    data class ShareDeckArchive(
        val filePath: String,
        val fileName: String,
    ) : DeckScreenEffect
}
