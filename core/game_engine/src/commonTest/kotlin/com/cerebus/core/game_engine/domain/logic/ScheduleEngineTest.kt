package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.SrsConfig
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleEngineTest {

    private val config = SrsConfig()
    private val submittedAt = 1_000_000L

    @Test
    fun scheduleNext_newAgain_learningStep0_duePlusFiveMinutes() {
        val result = scheduleNext(progress(CardState.NEW), Grade.AGAIN, submittedAt, config)
        assertEquals(CardState.LEARNING, result.updatedProgress.state)
        assertEquals(0, result.updatedProgress.learningStepIndex)
        assertEquals(submittedAt + 5 * 60_000L, result.dueAtEpochMillis)
    }

    @Test
    fun scheduleNext_newGood_learningStep1_duePlusThirtyMinutes() {
        val result = scheduleNext(progress(CardState.NEW), Grade.GOOD, submittedAt, config)
        assertEquals(CardState.LEARNING, result.updatedProgress.state)
        assertEquals(1, result.updatedProgress.learningStepIndex)
        assertEquals(submittedAt + 30 * 60_000L, result.dueAtEpochMillis)
    }

    @Test
    fun scheduleNext_learningStep1Good_goesToStep2_duePlusOneDay() {
        val result = scheduleNext(
            progress(
                state = CardState.LEARNING,
                step = 1,
            ),
            Grade.GOOD,
            submittedAt,
            config,
        )
        assertEquals(CardState.LEARNING, result.updatedProgress.state)
        assertEquals(2, result.updatedProgress.learningStepIndex)
        assertEquals(submittedAt + 24 * 60 * 60_000L, result.dueAtEpochMillis)
    }

    @Test
    fun scheduleNext_learningLastStepGood_movesToReview_intervalTwoDays() {
        val result = scheduleNext(
            progress(
                state = CardState.LEARNING,
                step = config.learningStepsMillis.lastIndex,
            ),
            Grade.GOOD,
            submittedAt,
            config,
        )
        assertEquals(CardState.REVIEW, result.updatedProgress.state)
        assertEquals(2.0, result.updatedProgress.intervalDays)
        assertEquals(submittedAt + daysToMillis(2.0), result.dueAtEpochMillis)
    }

    @Test
    fun scheduleNext_reviewI4E2dot3Good_interval9dot2_duePlus9dot2Days() {
        val result = scheduleNext(
            progress(
                state = CardState.REVIEW,
                intervalDays = 4.0,
                ease = 2.3,
            ),
            Grade.GOOD,
            submittedAt,
            config,
        )
        assertEquals(CardState.REVIEW, result.updatedProgress.state)
        assertEquals(9.2, result.updatedProgress.intervalDays)
        assertEquals(submittedAt + daysToMillis(9.2), result.dueAtEpochMillis)
    }

    @Test
    fun scheduleNext_reviewI10Again_relearningInterval5_duePlus5Minutes() {
        val result = scheduleNext(
            progress(
                state = CardState.REVIEW,
                intervalDays = 10.0,
                ease = 2.3,
            ),
            Grade.AGAIN,
            submittedAt,
            config,
        )
        assertEquals(CardState.RELEARNING, result.updatedProgress.state)
        assertEquals(5.0, result.updatedProgress.intervalDays)
        assertEquals(submittedAt + 5 * 60_000L, result.dueAtEpochMillis)
    }

    @Test
    fun scheduleNext_relearningLastStepGood_returnsReview_duePlusIntervalDays() {
        val result = scheduleNext(
            progress(
                state = CardState.RELEARNING,
                step = config.relearningStepsMillis.lastIndex,
                intervalDays = 5.0,
            ),
            Grade.GOOD,
            submittedAt,
            config,
        )
        assertEquals(CardState.REVIEW, result.updatedProgress.state)
        assertEquals(submittedAt + daysToMillis(5.0), result.dueAtEpochMillis)
    }

    private fun progress(
        state: CardState,
        step: Int = 0,
        intervalDays: Double = 0.0,
        ease: Double = 2.3,
    ): CardProgress {
        return CardProgress(
            studentId = "s1",
            cardId = "c1",
            state = state,
            dueAtEpochMillis = submittedAt,
            intervalDays = intervalDays,
            ease = ease,
            learningStepIndex = step,
            reps = 0,
            lapses = 0,
            lastReviewedAtEpochMillis = null,
            lastGrade = null,
        )
    }

    private fun daysToMillis(days: Double): Long {
        return (days * 24 * 60 * 60 * 1000.0).roundToLong()
    }
}
