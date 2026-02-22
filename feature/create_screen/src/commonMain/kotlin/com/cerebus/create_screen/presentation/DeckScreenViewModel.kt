package com.cerebus.create_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.utils.UniqueIdGenerator
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeckScreenViewModel(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
) : ViewModel() {

    private companion object {
        const val MAX_DECK_NAME_LENGTH = 40
    }

    private val _uiState = MutableStateFlow(DeckUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()

    fun onAction(action: DeckScreenAction) {
        when (action) {
            is DeckScreenAction.Initialize -> loadDeck(action.deckId)
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

            DeckScreenAction.OnSaveNameClick -> updateDeckName()
            DeckScreenAction.OnEditCoverClick -> {
                _uiState.update { it.copy(isEditCoverSourceDialogVisible = true) }
            }

            DeckScreenAction.OnDismissEditCoverSourceDialog -> {
                _uiState.update { it.copy(isEditCoverSourceDialogVisible = false) }
            }

            DeckScreenAction.OnPickCoverFromGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isEditCoverSourceDialogVisible = false,
                        pendingPickerRequest = DeckPickerRequest.GALLERY,
                        pendingPickerTarget = DeckPickerTarget.DECK_COVER,
                    )
                }
            }

            DeckScreenAction.OnTakeCoverPhotoClick -> {
                _uiState.update {
                    it.copy(
                        isEditCoverSourceDialogVisible = false,
                        pendingPickerRequest = DeckPickerRequest.CAMERA,
                        pendingPickerTarget = DeckPickerTarget.DECK_COVER,
                    )
                }
            }

            DeckScreenAction.OnPickerRequestConsumed -> {
                _uiState.update { it.copy(pendingPickerRequest = null) }
            }

            is DeckScreenAction.OnImagePicked -> onImagePicked(action.uri)
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
                        cardEditorMode = CardEditorMode.CREATE,
                        editingCardId = null,
                        isCardCoverSourceDialogVisible = false,
                        cardName = "",
                        cardImageUrl = null,
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

            DeckScreenAction.OnPickCardCoverFromGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isCardCoverSourceDialogVisible = false,
                        pendingPickerRequest = DeckPickerRequest.GALLERY,
                        pendingPickerTarget = DeckPickerTarget.CARD_IMAGE,
                    )
                }
            }

            DeckScreenAction.OnTakeCardCoverPhotoClick -> {
                _uiState.update {
                    it.copy(
                        isCardCoverSourceDialogVisible = false,
                        pendingPickerRequest = DeckPickerRequest.CAMERA,
                        pendingPickerTarget = DeckPickerTarget.CARD_IMAGE,
                    )
                }
            }

            DeckScreenAction.OnConfirmAddCard -> saveCard()
            is DeckScreenAction.OnCardLongPress -> toggleCardSelection(action.cardId)
            is DeckScreenAction.OnCardClick -> onCardClick(action.cardId)
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

    private fun loadDeck(deckId: String) {
        if (_uiState.value.deckId == deckId && !_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, deckId = deckId) }
            val deck = deckRepository.getDeckById(deckId)
            if (deck != null) {
                _uiState.update {
                    it.copy(
                        deckName = deck.name,
                        coverUri = deck.coverUri,
                        editingName = deck.name,
                        isLoading = false,
                        validationError = null,
                    )
                }
                loadFlashcards(deckId)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        validationError = DeckValidationError.DECK_NOT_FOUND,
                    )
                }
            }
        }
    }

    private fun loadFlashcards(deckId: String) {
        viewModelScope.launch {
            val flashcards = flashcardRepository.getFlashcardsByDeckId(deckId)
            _uiState.update { it.copy(flashcards = flashcards) }
        }
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
                        flashcards = it.flashcards + newCard,
                        isCardSaving = false,
                        isAddCardDialogVisible = false,
                        cardEditorMode = CardEditorMode.CREATE,
                        editingCardId = null,
                        isCardCoverSourceDialogVisible = false,
                        cardName = "",
                        cardImageUrl = null,
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
                        flashcards = it.flashcards.map { card ->
                            if (card.id == cardId) updatedCard else card
                        },
                        isCardSaving = false,
                        isAddCardDialogVisible = false,
                        cardEditorMode = CardEditorMode.CREATE,
                        editingCardId = null,
                        isCardCoverSourceDialogVisible = false,
                        cardName = "",
                        cardImageUrl = null,
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
                    flashcards = state.flashcards.filterNot { it.id in deletedIds },
                    selectedCardIds = remainingSelection,
                    isDeletingSelectedCards = false,
                    isDeleteSelectedDialogVisible = false,
                    validationError = if (hasFailures) DeckValidationError.DELETE_CARDS_FAILED else null,
                )
            }
        }
    }

    private fun onImagePicked(uri: String) {
        when (_uiState.value.pendingPickerTarget) {
            DeckPickerTarget.DECK_COVER -> updateDeckCover(uri)
            DeckPickerTarget.CARD_IMAGE -> _uiState.update {
                it.copy(
                    cardImageUrl = uri,
                    pendingPickerTarget = null,
                    validationError = null,
                )
            }
            null -> Unit
        }
        _uiState.update { it.copy(pendingPickerTarget = null) }
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
                DeckNavigationState.notifyDeckChanged()
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

    private fun updateDeckCover(uri: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val updated = deckRepository.updateDeckCoverUri(state.deckId, uri)
            if (updated) {
                DeckNavigationState.notifyDeckChanged()
                _uiState.update {
                    it.copy(
                        coverUri = uri,
                        pendingPickerTarget = null,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update { it.copy(validationError = DeckValidationError.UPDATE_COVER_FAILED) }
            }
        }
    }
}
