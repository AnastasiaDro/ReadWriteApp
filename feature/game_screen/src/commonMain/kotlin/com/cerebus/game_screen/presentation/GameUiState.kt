package com.cerebus.game_screen.presentation

sealed interface GameUiState {

    data object Loading : GameUiState

    data object StartGame : GameUiState

    data class ActiveGame(
        val pictureUrl: String,
        val currentAnswer: String,
        val validAnswer: String,
        val isKeyboardActive: Boolean,
    ) : GameUiState
}