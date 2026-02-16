package com.cerebus.create_screen.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeckNavigationState {
    var selectedDeckId: String = ""

    private val _deckChangedVersion = MutableStateFlow(0)
    val deckChangedVersion: StateFlow<Int> = _deckChangedVersion.asStateFlow()

    fun notifyDeckChanged() {
        _deckChangedVersion.value = _deckChangedVersion.value + 1
    }
}
