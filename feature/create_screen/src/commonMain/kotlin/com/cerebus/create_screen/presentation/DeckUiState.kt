package com.cerebus.create_screen.presentation

import com.cerebus.core.game_engine.domain.logic.SrsAvailability
import com.cerebus.data.flashcards.domain.models.Flashcard

enum class DeckPickerRequest {
    GALLERY,
    CAMERA,
}

enum class DeckPickerTarget {
    DECK_COVER,
    CARD_IMAGE,
}

enum class CardEditorMode {
    CREATE,
    EDIT,
}

enum class DeckValidationError {
    DECK_NOT_FOUND,
    EMPTY_DECK_NAME,
    UPDATE_NAME_FAILED,
    UPDATE_COVER_FAILED,
    EMPTY_CARD_NAME,
    ADD_CARD_FAILED,
    UPDATE_CARD_FAILED,
    DELETE_CARDS_FAILED,
}

data class DeckUiState(
    val deckId: String = "",
    val deckName: String = "",
    val coverUri: String? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val isEditNameDialogVisible: Boolean = false,
    val isEditCoverSourceDialogVisible: Boolean = false,
    val pendingPickerRequest: DeckPickerRequest? = null,
    val pendingPickerTarget: DeckPickerTarget? = null,
    val isAddCardDialogVisible: Boolean = false,
    val cardEditorMode: CardEditorMode = CardEditorMode.CREATE,
    val editingCardId: String? = null,
    val isCardCoverSourceDialogVisible: Boolean = false,
    val cardName: String = "",
    val cardImageUrl: String? = null,
    val isCardSaving: Boolean = false,
    val isDeleteSelectedDialogVisible: Boolean = false,
    val isCardSelectionMode: Boolean = false,
    val selectedCardIds: Set<String> = emptySet(),
    val isDeletingSelectedCards: Boolean = false,
    val editingName: String = "",
    val validationError: DeckValidationError? = null,
    val flashcards: List<Flashcard> = emptyList(),
    val srsAvailability: SrsAvailability? = null,
)
