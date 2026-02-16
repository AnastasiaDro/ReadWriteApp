package com.cerebus.create_screen.presentation

import com.cerebus.decks.domain.models.Deck

enum class CreateValidationError {
    EMPTY_DECK_NAME,
    CREATE_DECK_FAILED,
    DELETE_DECK_FAILED,
}

data class CreateUiState(
    val decks: List<Deck> = emptyList(),
    val isCreateDialogVisible: Boolean = false,
    val isCoverSourceDialogVisible: Boolean = false,
    val isSuccessDialogVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val deckPendingDelete: Deck? = null,
    val createdDeckName: String = "",
    val createdDeckCoverUri: String? = null,
    val deckName: String = "",
    val coverUri: String? = null,
    val isSaving: Boolean = false,
    val validationError: CreateValidationError? = null,
)
