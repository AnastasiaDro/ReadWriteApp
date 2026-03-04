package com.cerebus.game_screen.presentation

sealed interface GameScreenAction {
    data class OnAnswerChanged(val value: String) : GameScreenAction
    data object OnCheckClick : GameScreenAction
    data object OnRetryClick : GameScreenAction
    data object OnBackToStudentClick : GameScreenAction
}

sealed interface GameScreenEffect {
    data object OpenActiveStudent : GameScreenEffect
}
