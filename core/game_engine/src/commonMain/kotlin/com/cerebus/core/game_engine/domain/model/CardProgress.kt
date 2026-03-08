package com.cerebus.core.game_engine.domain.model

data class CardProgress(
    val studentId: String,
    val cardId: String,
    val state: CardState,
    val dueAtEpochMillis: Long,
    val intervalDays: Double,
    val ease: Double,
    val learningStepIndex: Int,
    val reps: Int,
    val lapses: Int,
    val lastReviewedAtEpochMillis: Long?,
    val lastGrade: Grade?,
    val guidedHintSuccessCount: Int = 0,
)
