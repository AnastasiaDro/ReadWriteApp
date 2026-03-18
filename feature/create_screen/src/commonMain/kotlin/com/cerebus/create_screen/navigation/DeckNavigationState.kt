package com.cerebus.create_screen.navigation

object DeckNavigationState {
    var selectedDeckId: String = ""
        private set

    private var openAddCardDialogOnNextOpen: Boolean = false
    private var openGalleryCardIdOnNextOpen: String? = null
    private var openEditCardIdOnNextOpen: String? = null
    private var reopenGalleryCardIdAfterEditorClose: String? = null

    fun selectDeck(
        deckId: String,
        openAddCardDialog: Boolean = false,
        openGalleryCardId: String? = null,
        openEditCardId: String? = null,
        reopenGalleryCardIdAfterEditorClose: String? = null,
    ) {
        selectedDeckId = deckId
        openAddCardDialogOnNextOpen = openAddCardDialog
        openGalleryCardIdOnNextOpen = openGalleryCardId
        openEditCardIdOnNextOpen = openEditCardId
        this.reopenGalleryCardIdAfterEditorClose = reopenGalleryCardIdAfterEditorClose
    }

    fun consumeOpenAddCardDialogOnNextOpen(): Boolean {
        val shouldOpen = openAddCardDialogOnNextOpen
        openAddCardDialogOnNextOpen = false
        return shouldOpen
    }

    fun consumeOpenGalleryCardIdOnNextOpen(): String? {
        val cardId = openGalleryCardIdOnNextOpen
        openGalleryCardIdOnNextOpen = null
        return cardId
    }

    fun consumeOpenEditCardIdOnNextOpen(): String? {
        val cardId = openEditCardIdOnNextOpen
        openEditCardIdOnNextOpen = null
        return cardId
    }

    fun consumeReopenGalleryCardIdAfterEditorClose(): String? {
        val cardId = reopenGalleryCardIdAfterEditorClose
        reopenGalleryCardIdAfterEditorClose = null
        return cardId
    }
}
