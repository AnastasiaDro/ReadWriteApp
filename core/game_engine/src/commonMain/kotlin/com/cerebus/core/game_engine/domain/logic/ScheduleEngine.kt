package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.ScheduleResult
import com.cerebus.core.game_engine.domain.model.SrsConfig

fun scheduleNext(
    progress: CardProgress,
    hintLevel: Int,
    submittedAtEpochMillis: Long,
    config: SrsConfig,
): ScheduleResult {
    val normalizedHintLevel = hintLevel.coerceIn(0, 3)
    val requiredSuccesses = config.requiredRecallSuccesses.coerceAtLeast(1)

    return when (normalizedHintLevel) {
        0, 1 -> {
            val nextStreak = progress.recallSuccessStreak + 1
            if (nextStreak >= requiredSuccesses) {
                val nextLevel = (progress.level + 1).coerceIn(0, config.maxLevel)
                val dueAt = submittedAtEpochMillis + levelIntervalMillis(nextLevel, config)
                val updated = progress.copy(
                    level = nextLevel,
                    dueAtEpochMillis = dueAt,
                    recallSuccessStreak = 0,
                    lastReviewedAtEpochMillis = submittedAtEpochMillis,
                )
                ScheduleResult(
                    updatedProgress = updated,
                    dueAtEpochMillis = dueAt,
                    leveledUp = nextLevel > progress.level,
                )
            } else {
                val dueAt = submittedAtEpochMillis + levelIntervalMillis(progress.level, config)
                val updated = progress.copy(
                    dueAtEpochMillis = dueAt,
                    recallSuccessStreak = nextStreak,
                    lastReviewedAtEpochMillis = submittedAtEpochMillis,
                )
                ScheduleResult(
                    updatedProgress = updated,
                    dueAtEpochMillis = dueAt,
                    leveledUp = false,
                )
            }
        }

        2 -> {
            val nextLevel = (progress.level - 1).coerceAtLeast(0)
            val dueAt = submittedAtEpochMillis + levelIntervalMillis(nextLevel, config)
            val updated = progress.copy(
                level = nextLevel,
                dueAtEpochMillis = dueAt,
                recallSuccessStreak = 0,
                lastReviewedAtEpochMillis = submittedAtEpochMillis,
            )
            ScheduleResult(
                updatedProgress = updated,
                dueAtEpochMillis = dueAt,
                leveledUp = false,
            )
        }

        else -> {
            val dueAt = submittedAtEpochMillis + levelIntervalMillis(0, config)
            val updated = progress.copy(
                level = 0,
                dueAtEpochMillis = dueAt,
                recallSuccessStreak = 0,
                lastReviewedAtEpochMillis = submittedAtEpochMillis,
            )
            ScheduleResult(
                updatedProgress = updated,
                dueAtEpochMillis = dueAt,
                leveledUp = false,
            )
        }
    }
}

private fun levelIntervalMillis(
    level: Int,
    config: SrsConfig,
): Long {
    val safeLevel = level.coerceIn(0, config.levelIntervalsMillis.lastIndex)
    return config.levelIntervalsMillis[safeLevel]
}
