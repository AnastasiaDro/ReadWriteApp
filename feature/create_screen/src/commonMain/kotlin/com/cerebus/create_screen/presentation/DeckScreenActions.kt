package com.cerebus.create_screen.presentation

sealed interface DeckScreenAction {
    data class Initialize(val deckId: String) : DeckScreenAction
    data object OnEditNameClick : DeckScreenAction
    data object OnDismissEditNameDialog : DeckScreenAction
    data class OnNameChanged(val value: String) : DeckScreenAction
    data object OnSaveNameClick : DeckScreenAction

    data object OnEditCoverClick : DeckScreenAction
    data object OnDismissEditCoverSourceDialog : DeckScreenAction
    data object OnPickCoverFromGalleryClick : DeckScreenAction
    data object OnTakeCoverPhotoClick : DeckScreenAction
    data object OnPickerRequestConsumed : DeckScreenAction
    data class OnImagePicked(val uri: String) : DeckScreenAction

    data object OnAddCardClick : DeckScreenAction
    data object OnDismissAddCardDialog : DeckScreenAction
    data class OnCardNameChanged(val value: String) : DeckScreenAction
    data object OnCardCoverButtonClick : DeckScreenAction
    data object OnDismissCardCoverSourceDialog : DeckScreenAction
    data object OnPickCardCoverFromGalleryClick : DeckScreenAction
    data object OnTakeCardCoverPhotoClick : DeckScreenAction
    data object OnConfirmAddCard : DeckScreenAction
}
