package com.cerebus.create_screen.presentation

sealed interface CreateScreenAction {
    data object OnCreateDeckClick : CreateScreenAction
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
    data class OpenDeck(
        val deckId: String,
        val openAddCardDialog: Boolean = false,
    ) : CreateScreenEffect
}
