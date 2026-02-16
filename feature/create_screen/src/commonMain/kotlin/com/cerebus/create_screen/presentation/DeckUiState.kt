package com.cerebus.create_screen.presentation

enum class DeckPickerRequest {
    GALLERY,
    CAMERA,
}

enum class DeckValidationError {
    DECK_NOT_FOUND,
    EMPTY_DECK_NAME,
    UPDATE_NAME_FAILED,
    UPDATE_COVER_FAILED,
}

data class DeckUiState(
    val deckId: String = "",
    val deckName: String = "",
    val coverUri: String? = null,
    val isLoading: Boolean = true,
    val isEditNameDialogVisible: Boolean = false,
    val isEditCoverSourceDialogVisible: Boolean = false,
    val pendingPickerRequest: DeckPickerRequest? = null,
    val editingName: String = "",
    val validationError: DeckValidationError? = null,
    val flashcardsPlaceholderCount: Int = 12,
)
