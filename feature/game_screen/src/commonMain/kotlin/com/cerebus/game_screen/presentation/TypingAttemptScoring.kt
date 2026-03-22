package com.cerebus.game_screen.presentation

enum class TypingLearningStage {
    Copy,
    Recall,
}

const val DEFAULT_FREE_NEIGHBOR_SLIP_PRESSES = 1

enum class HintLevel(val value: Int) {
    None(0),
    WrongPresses(1),
    ShowWord(1),
    InputHint(2),
    SimplifiedKeyboard(3),
}

data class TypingAttemptMetrics(
    val wrongPressCount: Int = 0,
    val slipPressCount: Int = 0,
    val freeSlipPresses: Int = 1,
    val usedInputHint: Boolean = false,
    val usedShowWord: Boolean = false,
    val usedSimplifiedKeyboard: Boolean = false,
)

fun computeHintLevel(metrics: TypingAttemptMetrics): HintLevel {
    return when {
        metrics.usedSimplifiedKeyboard -> HintLevel.SimplifiedKeyboard
        metrics.usedInputHint -> HintLevel.InputHint
        metrics.usedShowWord -> HintLevel.ShowWord
        metrics.wrongPressCount > 0 -> HintLevel.WrongPresses
        metrics.slipPressCount > metrics.freeSlipPresses.coerceAtLeast(0) -> HintLevel.WrongPresses
        else -> HintLevel.None
    }
}
