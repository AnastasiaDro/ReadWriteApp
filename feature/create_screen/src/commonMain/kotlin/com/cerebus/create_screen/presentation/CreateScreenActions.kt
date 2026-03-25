package com.cerebus.create_screen.presentation

import com.cerebus.core.deck_package.domain.service.DeckPackageExportFile

sealed interface CreateScreenAction {
    data object OnCreateDeckClick : CreateScreenAction
    data object OnImportDeckClick : CreateScreenAction
    data class OnImportDeckFilePicked(val uri: String) : CreateScreenAction
    data object OnConfirmImportDeckReplacement : CreateScreenAction
    data object OnConfirmImportDeckAddMissingCards : CreateScreenAction
    data object OnDismissImportDeckReplacement : CreateScreenAction
    data object OnDismissCreateDialog : CreateScreenAction

    data class OnDeckNameChanged(val value: String) : CreateScreenAction
    data class OnCoverUriSelected(val uri: String) : CreateScreenAction

    data object OnCoverButtonClick : CreateScreenAction
    data object OnDismissCoverSourceDialog : CreateScreenAction
    data object OnPickFromGalleryClick : CreateScreenAction
    data object OnTakePhotoClick : CreateScreenAction

    data object OnConfirmCreateDeck : CreateScreenAction

    data class OnDeckClick(val deckId: String) : CreateScreenAction
    data class OnDeckLongClick(val deckId: String) : CreateScreenAction
    data object OnExportSelectedDecksClick : CreateScreenAction
    data object OnDeleteSelectedDecksClick : CreateScreenAction
    data object OnDismissDeleteSelectedDialog : CreateScreenAction
    data object OnConfirmDeleteSelectedDecks : CreateScreenAction
    data object OnClearDeckSelection : CreateScreenAction

    data object OnAddCardsClick : CreateScreenAction
    data object OnCloseSuccessDialog : CreateScreenAction
}

sealed interface CreateScreenEffect {
    data object OpenGallery : CreateScreenEffect
    data object OpenCamera : CreateScreenEffect
    data object OpenImportDeckPicker : CreateScreenEffect
    data object ShowImportDeckFailed : CreateScreenEffect
    data object ShowExportSelectedDecksFailed : CreateScreenEffect
    data object ShowExportSelectedDecksLimitExceeded : CreateScreenEffect
    data class ShareDeckArchives(val files: List<DeckPackageExportFile>) : CreateScreenEffect
    data class OpenDeck(
        val deckId: String,
        val openAddCardDialog: Boolean = false,
    ) : CreateScreenEffect
}
