package com.cerebus.create_screen.presentation

sealed interface DeckScreenAction {
    data class Initialize(val deckId: String) : DeckScreenAction
    data object OnExportDeckClick : DeckScreenAction
    data object OnEditNameClick : DeckScreenAction
    data object OnDismissEditNameDialog : DeckScreenAction
    data class OnNameChanged(val value: String) : DeckScreenAction
    data object OnSaveNameClick : DeckScreenAction
    data object OnStartTrainingClick : DeckScreenAction
    data object OnStartRandomLearnedClick : DeckScreenAction
    data object OnStartRandomAllClick : DeckScreenAction
    data object OnOpenGalleryClick : DeckScreenAction

    data object OnEditCoverClick : DeckScreenAction
    data object OnDismissEditCoverSourceDialog : DeckScreenAction
    data class OnPickImageFromGallery(val target: DeckPickerTarget) : DeckScreenAction
    data class OnTakeImagePhoto(val target: DeckPickerTarget) : DeckScreenAction
    data object OnPickerRequestConsumed : DeckScreenAction
    data class OnImagePicked(
        val uri: String,
        val target: DeckPickerTarget? = null,
    ) : DeckScreenAction

    data object OnAddCardClick : DeckScreenAction
    data object OnDismissAddCardDialog : DeckScreenAction
    data class OnCardNameChanged(val value: String) : DeckScreenAction
    data object OnCardCoverButtonClick : DeckScreenAction
    data object OnDismissCardCoverSourceDialog : DeckScreenAction
    data object OnConfirmAddCard : DeckScreenAction

    data class OnCardLongPress(val cardId: String) : DeckScreenAction
    data class OnCardClick(val cardId: String) : DeckScreenAction
    data class OnOpenCardEditor(val cardId: String) : DeckScreenAction
    data object OnDeleteSelectedCardsClick : DeckScreenAction
    data object OnDismissDeleteSelectedCardsDialog : DeckScreenAction
    data object OnConfirmDeleteSelectedCards : DeckScreenAction
    data object OnClearCardSelection : DeckScreenAction
}
