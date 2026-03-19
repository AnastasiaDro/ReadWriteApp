package com.cerebus.game_screen.presentation

import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType

sealed interface GameUiState {

    data object Loading : GameUiState

    data class Active(
        val deckTitle: String,
        val currentCard: CardUi,
        val cardIndex: Int,
        val totalCards: Int,
        val studentId: String,
        val isPracticeMode: Boolean,
        val learningStage: TypingLearningStage,
        val activeSymbols: Set<String>,
        val preventWrongKeyPress: Boolean,
        val allowNeighborTypos: Boolean,
        val isShiftEnabled: Boolean,
        val hideDigitsOnTightScreen: Boolean,
        val keyboardFeedbackKey: String? = null,
        val keyboardFeedbackType: TrainingKeyboardFeedbackType? = null,
        val inputFeedbackType: TrainingKeyboardFeedbackType? = null,
        val answerInput: String,
        val isHintVisible: Boolean,
        val isSimplifiedKeyboardEnabled: Boolean = false,
        val copySuccessStreak: Int = 0,
        val wrongPressCount: Int = 0,
        val slipPressCount: Int = 0,
        val usedShowWord: Boolean = false,
        val usedSimplifiedKeyboard: Boolean = false,
        val showTypoSettingsSuggestion: Boolean = false,
        val lastHintLevel: HintLevel? = null,
        val feedback: FeedbackUi? = null,
    ) : GameUiState

    data class Finished(
        val deckTitle: String,
        val totalCards: Int,
        val correctAnswers: Int,
    ) : GameUiState
}

data class CardUi(
    val id: String,
    val answer: String,
    val imagePath: String?,
)

data class FeedbackUi(
    val message: String,
    val emoji: String,
)
