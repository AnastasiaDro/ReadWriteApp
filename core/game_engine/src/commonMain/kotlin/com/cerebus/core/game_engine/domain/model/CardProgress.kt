package com.cerebus.core.game_engine.domain.model

data class CardProgress(
    val studentId: String,
    val cardId: String,
    val level: Int,
    val dueAtEpochMillis: Long,
    val recallSuccessStreak: Int,
    val copySuccessStreak: Int,
    val lastReviewedAtEpochMillis: Long?,
    val lastHintLevel: Int?,
    val lastDurationMs: Long?,
    val lastWrongPressCount: Int = 0,
)
