package com.cerebus.game_screen.presentation

sealed interface GameUiState {

    data object Loading : GameUiState

    data class Active(
        val deckTitle: String,
        val currentCard: CardUi,
        val cardIndex: Int,
        val totalCards: Int,
        val studentId: String,
        val activeSymbols: Set<String>,
        val isShiftEnabled: Boolean,
        val answerInput: String,
        val isHintVisible: Boolean,
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
