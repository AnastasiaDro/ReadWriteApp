package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.SrsConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleEngineTest {

    private val config = SrsConfig()
    private val submittedAt = 1_000_000L

    @Test
    fun scheduleNext_hintLevelZero_beforeThreshold_incrementsRecallStreak() {
        val result = scheduleNext(
            progress(level = 2, recallSuccessStreak = 0),
            hintLevel = 0,
            submittedAtEpochMillis = submittedAt,
            config = config,
        )

        assertEquals(2, result.updatedProgress.level)
        assertEquals(1, result.updatedProgress.recallSuccessStreak)
        assertEquals(submittedAt + config.levelIntervalsMillis[2], result.dueAtEpochMillis)
        assertFalse(result.leveledUp)
    }

    @Test
    fun scheduleNext_hintLevelOne_onThreshold_levelsUpAndResetsStreak() {
        val result = scheduleNext(
            progress(level = 2, recallSuccessStreak = 1),
            hintLevel = 1,
            submittedAtEpochMillis = submittedAt,
            config = config,
        )

        assertEquals(3, result.updatedProgress.level)
        assertEquals(0, result.updatedProgress.recallSuccessStreak)
        assertEquals(submittedAt + config.levelIntervalsMillis[3], result.dueAtEpochMillis)
        assertTrue(result.leveledUp)
    }

    @Test
    fun scheduleNext_hintLevelTwo_decrementsLevelAndResetsStreak() {
        val result = scheduleNext(
            progress(level = 3, recallSuccessStreak = 1),
            hintLevel = 2,
            submittedAtEpochMillis = submittedAt,
            config = config,
        )

        assertEquals(2, result.updatedProgress.level)
        assertEquals(0, result.updatedProgress.recallSuccessStreak)
        assertEquals(submittedAt + config.levelIntervalsMillis[2], result.dueAtEpochMillis)
        assertFalse(result.leveledUp)
    }

    @Test
    fun scheduleNext_hintLevelThree_resetsCardToLevelZero() {
        val result = scheduleNext(
            progress(level = 4, recallSuccessStreak = 1),
            hintLevel = 3,
            submittedAtEpochMillis = submittedAt,
            config = config,
        )

        assertEquals(0, result.updatedProgress.level)
        assertEquals(0, result.updatedProgress.recallSuccessStreak)
        assertEquals(submittedAt + config.levelIntervalsMillis[0], result.dueAtEpochMillis)
        assertFalse(result.leveledUp)
    }

    @Test
    fun scheduleNext_doesNotLevelAboveConfiguredMaximum() {
        val result = scheduleNext(
            progress(level = config.maxLevel, recallSuccessStreak = config.requiredRecallSuccesses - 1),
            hintLevel = 0,
            submittedAtEpochMillis = submittedAt,
            config = config,
        )

        assertEquals(config.maxLevel, result.updatedProgress.level)
        assertEquals(0, result.updatedProgress.recallSuccessStreak)
        assertEquals(submittedAt + config.levelIntervalsMillis[config.maxLevel], result.dueAtEpochMillis)
        assertFalse(result.leveledUp)
    }

    private fun progress(
        level: Int,
        recallSuccessStreak: Int,
    ): CardProgress {
        return CardProgress(
            studentId = "s1",
            cardId = "c1",
            level = level,
            dueAtEpochMillis = submittedAt,
            recallSuccessStreak = recallSuccessStreak,
            copySuccessStreak = 0,
            lastReviewedAtEpochMillis = null,
            lastHintLevel = null,
            lastDurationMs = null,
            lastWrongPressCount = 0,
        )
    }
}
