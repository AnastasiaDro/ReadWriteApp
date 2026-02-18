package com.cerebus.create_screen.presentation

import com.cerebus.flashcards.domain.models.Flashcard

enum class DeckPickerRequest {
    GALLERY,
    CAMERA,
}

enum class DeckPickerTarget {
    DECK_COVER,
    CARD_IMAGE,
}

enum class DeckValidationError {
    DECK_NOT_FOUND,
    EMPTY_DECK_NAME,
    UPDATE_NAME_FAILED,
    UPDATE_COVER_FAILED,
    EMPTY_CARD_NAME,
    ADD_CARD_FAILED,
}

data class DeckUiState(
    val deckId: String = "",
    val deckName: String = "",
    val coverUri: String? = null,
    val isLoading: Boolean = true,
    val isEditNameDialogVisible: Boolean = false,
    val isEditCoverSourceDialogVisible: Boolean = false,
    val pendingPickerRequest: DeckPickerRequest? = null,
    val pendingPickerTarget: DeckPickerTarget? = null,
    val isAddCardDialogVisible: Boolean = false,
    val isCardCoverSourceDialogVisible: Boolean = false,
    val cardName: String = "",
    val cardImageUrl: String? = null,
    val isCardSaving: Boolean = false,
    val editingName: String = "",
    val validationError: DeckValidationError? = null,
    val flashcards: List<Flashcard> = emptyList(),
)
