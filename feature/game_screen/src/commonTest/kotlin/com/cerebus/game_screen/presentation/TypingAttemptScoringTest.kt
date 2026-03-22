package com.cerebus.game_screen.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class TypingAttemptScoringTest {
    @Test
    fun inputHint_countsAsShowWordLevelHint() {
        val result = computeHintLevel(
            TypingAttemptMetrics(
                usedInputHint = true,
            )
        )

        assertEquals(HintLevel.InputHint, result)
    }

    @Test
    fun showWord_staysSofterThanInputHint() {
        val result = computeHintLevel(
            TypingAttemptMetrics(
                usedShowWord = true,
            )
        )

        assertEquals(HintLevel.ShowWord, result)
    }

    @Test
    fun simplifiedKeyboard_staysStrongerThanInputHint() {
        val result = computeHintLevel(
            TypingAttemptMetrics(
                usedInputHint = true,
                usedSimplifiedKeyboard = true,
            )
        )

        assertEquals(HintLevel.SimplifiedKeyboard, result)
    }
}
