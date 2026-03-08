package com.cerebus.create_screen.navigation

object DeckNavigationState {
    var selectedDeckId: String = ""
        private set

    private var openAddCardDialogOnNextOpen: Boolean = false

    fun selectDeck(
        deckId: String,
        openAddCardDialog: Boolean = false,
    ) {
        selectedDeckId = deckId
        openAddCardDialogOnNextOpen = openAddCardDialog
    }

    fun consumeOpenAddCardDialogOnNextOpen(): Boolean {
        val shouldOpen = openAddCardDialogOnNextOpen
        openAddCardDialogOnNextOpen = false
        return shouldOpen
    }
}
