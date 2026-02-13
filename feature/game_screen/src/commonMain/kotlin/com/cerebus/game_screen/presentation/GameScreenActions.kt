package com.cerebus.game_screen.presentation

sealed interface GameScreenActions {

    data object OnBackPress : GameScreenActions

    data object FinishGame : GameScreenActions

    data class StartGame(val gameId: String) : GameScreenActions
}