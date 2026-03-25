package com.cerebus.create_screen.presentation

import com.cerebus.data.decks.domain.models.Deck

enum class CreateValidationError {
    EMPTY_DECK_NAME,
    CREATE_DECK_FAILED,
    DELETE_DECK_FAILED,
    DELETE_DECKS_FAILED,
}

data class PendingDeckImportConfirmation(
    val archiveUri: String,
    val importedDeckName: String,
    val existingDeckName: String,
    val matchingCardsCount: Int,
    val newCardsCount: Int,
    val staleCardsCount: Int,
)

data class CreateUiState(
    val decks: List<Deck> = emptyList(),
    val isCreateDialogVisible: Boolean = false,
    val isCoverSourceDialogVisible: Boolean = false,
    val isSuccessDialogVisible: Boolean = false,
    val isDeleteSelectedDialogVisible: Boolean = false,
    val selectedDeckIds: Set<String> = emptySet(),
    val isDeletingSelectedDecks: Boolean = false,
    val isExportingSelectedDecks: Boolean = false,
    val createdDeckId: String = "",
    val createdDeckName: String = "",
    val createdDeckCoverUri: String? = null,
    val deckName: String = "",
    val coverUri: String? = null,
    val isSaving: Boolean = false,
    val validationError: CreateValidationError? = null,
    val pendingDeckImportConfirmation: PendingDeckImportConfirmation? = null,
)
