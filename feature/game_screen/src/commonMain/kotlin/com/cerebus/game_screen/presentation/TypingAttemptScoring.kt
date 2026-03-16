package com.cerebus.game_screen.presentation

enum class TypingLearningStage {
    Copy,
    Recall,
}

enum class HintLevel(val value: Int) {
    None(0),
    WrongPresses(1),
    ShowWord(2),
    SimplifiedKeyboard(3),
}

data class TypingAttemptMetrics(
    val wrongPressCount: Int = 0,
    val usedShowWord: Boolean = false,
    val usedSimplifiedKeyboard: Boolean = false,
)

fun computeHintLevel(metrics: TypingAttemptMetrics): HintLevel {
    return when {
        metrics.usedSimplifiedKeyboard -> HintLevel.SimplifiedKeyboard
        metrics.usedShowWord -> HintLevel.ShowWord
        metrics.wrongPressCount > 0 -> HintLevel.WrongPresses
        else -> HintLevel.None
    }
}
