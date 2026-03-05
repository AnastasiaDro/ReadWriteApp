package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.ScheduleResult
import com.cerebus.core.game_engine.domain.model.SrsConfig
import kotlin.math.max
import kotlin.math.roundToLong

private const val DAY_MILLIS = 24 * 60 * 60 * 1000.0

fun scheduleNext(
    progress: CardProgress,
    grade: Grade,
    submittedAtEpochMillis: Long,
    config: SrsConfig,
): ScheduleResult {
    val normalizedGrade = normalizeGradeForState(grade, progress.state)
    return when (progress.state) {
        CardState.NEW -> scheduleFromNew(progress, normalizedGrade, submittedAtEpochMillis, config)
        CardState.LEARNING -> scheduleFromLearning(progress, normalizedGrade, submittedAtEpochMillis, config)
        CardState.REVIEW -> scheduleFromReview(progress, normalizedGrade, submittedAtEpochMillis, config)
        CardState.RELEARNING -> scheduleFromRelearning(progress, normalizedGrade, submittedAtEpochMillis, config)
    }
}

private fun scheduleFromNew(
    progress: CardProgress,
    grade: Grade,
    submittedAt: Long,
    config: SrsConfig,
): ScheduleResult {
    val stepIndex = when (grade) {
        Grade.AGAIN -> 0
        Grade.GOOD, Grade.EASY -> minOf(1, config.learningStepsMillis.lastIndex)
    }
    val dueAt = submittedAt + stepMillis(config.learningStepsMillis, stepIndex)
    val updated = progress.copy(
        state = CardState.LEARNING,
        dueAtEpochMillis = dueAt,
        intervalDays = 0.0,
        ease = if (progress.ease <= 0.0) config.easeStart else progress.ease,
        learningStepIndex = stepIndex,
        lastReviewedAtEpochMillis = submittedAt,
        lastGrade = if (grade == Grade.EASY) Grade.GOOD else grade,
    )
    return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
}

private fun scheduleFromLearning(
    progress: CardProgress,
    grade: Grade,
    submittedAt: Long,
    config: SrsConfig,
): ScheduleResult {
    if (grade == Grade.AGAIN) {
        val dueAt = submittedAt + stepMillis(config.learningStepsMillis, 0)
        val updated = progress.copy(
            state = CardState.LEARNING,
            dueAtEpochMillis = dueAt,
            intervalDays = 0.0,
            learningStepIndex = 0,
            lastReviewedAtEpochMillis = submittedAt,
            lastGrade = Grade.AGAIN,
        )
        return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
    }

    val nextStep = progress.learningStepIndex + 1
    if (nextStep <= config.learningStepsMillis.lastIndex) {
        val dueAt = submittedAt + stepMillis(config.learningStepsMillis, nextStep)
        val updated = progress.copy(
            state = CardState.LEARNING,
            dueAtEpochMillis = dueAt,
            intervalDays = 0.0,
            learningStepIndex = nextStep,
            lastReviewedAtEpochMillis = submittedAt,
            lastGrade = Grade.GOOD,
        )
        return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
    }

    val interval = clampInterval(config.graduatingIntervalDays, config)
    val dueAt = submittedAt + daysToMillis(interval)
    val updated = progress.copy(
        state = CardState.REVIEW,
        dueAtEpochMillis = dueAt,
        intervalDays = interval,
        learningStepIndex = 0,
        reps = progress.reps + 1,
        lastReviewedAtEpochMillis = submittedAt,
        lastGrade = Grade.GOOD,
    )
    return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
}

private fun scheduleFromReview(
    progress: CardProgress,
    grade: Grade,
    submittedAt: Long,
    config: SrsConfig,
): ScheduleResult {
    val intervalBase = max(progress.intervalDays, config.minIntervalDays)
    val ease = effectiveEase(progress.ease, config)

    return when (grade) {
        Grade.AGAIN -> {
            val newInterval = clampInterval(intervalBase * config.lapseNewIntervalFactor, config)
            val dueAt = submittedAt + stepMillis(config.relearningStepsMillis, 0)
            val updated = progress.copy(
                state = CardState.RELEARNING,
                dueAtEpochMillis = dueAt,
                intervalDays = newInterval,
                learningStepIndex = 0,
                lapses = progress.lapses + 1,
                lastReviewedAtEpochMillis = submittedAt,
                lastGrade = Grade.AGAIN,
            )
            ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
        }

        Grade.GOOD -> {
            val nextInterval = clampInterval(intervalBase * ease, config)
            val dueAt = submittedAt + daysToMillis(nextInterval)
            val updated = progress.copy(
                state = CardState.REVIEW,
                dueAtEpochMillis = dueAt,
                intervalDays = nextInterval,
                learningStepIndex = 0,
                reps = progress.reps + 1,
                lastReviewedAtEpochMillis = submittedAt,
                lastGrade = Grade.GOOD,
            )
            ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
        }

        Grade.EASY -> {
            val nextInterval = clampInterval(intervalBase * ease * config.easyBonus, config)
            val dueAt = submittedAt + daysToMillis(nextInterval)
            val updated = progress.copy(
                state = CardState.REVIEW,
                dueAtEpochMillis = dueAt,
                intervalDays = nextInterval,
                learningStepIndex = 0,
                reps = progress.reps + 1,
                lastReviewedAtEpochMillis = submittedAt,
                lastGrade = Grade.EASY,
            )
            ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
        }
    }
}

private fun scheduleFromRelearning(
    progress: CardProgress,
    grade: Grade,
    submittedAt: Long,
    config: SrsConfig,
): ScheduleResult {
    if (grade == Grade.AGAIN) {
        val dueAt = submittedAt + stepMillis(config.relearningStepsMillis, 0)
        val updated = progress.copy(
            state = CardState.RELEARNING,
            dueAtEpochMillis = dueAt,
            learningStepIndex = 0,
            lastReviewedAtEpochMillis = submittedAt,
            lastGrade = Grade.AGAIN,
        )
        return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
    }

    val nextStep = progress.learningStepIndex + 1
    if (nextStep <= config.relearningStepsMillis.lastIndex) {
        val dueAt = submittedAt + stepMillis(config.relearningStepsMillis, nextStep)
        val updated = progress.copy(
            state = CardState.RELEARNING,
            dueAtEpochMillis = dueAt,
            learningStepIndex = nextStep,
            lastReviewedAtEpochMillis = submittedAt,
            lastGrade = Grade.GOOD,
        )
        return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
    }

    val interval = clampInterval(max(progress.intervalDays, config.minIntervalDays), config)
    val dueAt = submittedAt + daysToMillis(interval)
    val updated = progress.copy(
        state = CardState.REVIEW,
        dueAtEpochMillis = dueAt,
        intervalDays = interval,
        learningStepIndex = 0,
        reps = progress.reps + 1,
        lastReviewedAtEpochMillis = submittedAt,
        lastGrade = Grade.GOOD,
    )
    return ScheduleResult(updatedProgress = updated, dueAtEpochMillis = dueAt)
}

private fun normalizeGradeForState(
    grade: Grade,
    state: CardState,
): Grade {
    return if (grade == Grade.EASY && state != CardState.REVIEW) {
        Grade.GOOD
    } else {
        grade
    }
}

private fun effectiveEase(
    ease: Double,
    config: SrsConfig,
): Double {
    return if (ease <= 0.0) {
        config.easeStart
    } else {
        max(ease, config.easeMin)
    }
}

private fun stepMillis(
    steps: List<Long>,
    index: Int,
): Long {
    if (steps.isEmpty()) return 0L
    val safeIndex = index.coerceIn(0, steps.lastIndex)
    return steps[safeIndex]
}

private fun clampInterval(
    days: Double,
    config: SrsConfig,
): Double {
    return days.coerceIn(config.minIntervalDays, config.maxIntervalDays)
}

private fun daysToMillis(days: Double): Long {
    return (days * DAY_MILLIS).roundToLong()
}
