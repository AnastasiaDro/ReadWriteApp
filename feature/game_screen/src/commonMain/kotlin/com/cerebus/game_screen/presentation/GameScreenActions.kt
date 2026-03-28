package com.cerebus.game_screen.presentation

sealed interface GameScreenAction {
    data class OnAnswerChanged(val value: String) : GameScreenAction
    data class OnKeyboardSymbolPressed(val symbol: String) : GameScreenAction
    data object OnBackspacePressed : GameScreenAction
    data class OnShiftChanged(val isEnabled: Boolean) : GameScreenAction
    data class OnInputHintHelpToggled(val isEnabled: Boolean) : GameScreenAction
    data class OnShowWordHelpToggled(val isEnabled: Boolean) : GameScreenAction
    data class OnSimplifyKeyboardHelpToggled(val isEnabled: Boolean) : GameScreenAction
    data object OnTypoSuggestionDismissed : GameScreenAction
    data object OnCheckClick : GameScreenAction
    data object OnPlanClick : GameScreenAction
    data object OnRetryClick : GameScreenAction
    data object OnRandomReviewClick : GameScreenAction
    data object OnLearnMoreClick : GameScreenAction
    data object OnBackToStudentClick : GameScreenAction
    data object OnCloseClick : GameScreenAction
}

sealed interface GameScreenEffect {
    data object OpenActiveStudent : GameScreenEffect
    data class OpenSessionSettings(val studentId: String) : GameScreenEffect
    data object CloseGame : GameScreenEffect
}
