package com.cerebus.core.game_engine.domain.model

data class ReviewLog(
    val studentId: String,
    val cardId: String,
    val shownAtEpochMillis: Long,
    val submittedAtEpochMillis: Long,
    val userInputRaw: String,
    val userInputNormalized: String,
    val expectedAnswerNormalized: String,
    val isCorrect: Boolean,
    val hintLevel: Int,
    val wrongPressCount: Int,
    val durationMs: Long,
    val copyStage: Boolean,
    val levelBefore: Int,
    val levelAfter: Int,
    val recallSuccessStreakBefore: Int,
    val recallSuccessStreakAfter: Int,
    val copySuccessStreakBefore: Int,
    val copySuccessStreakAfter: Int,
    val dueAtBeforeEpochMillis: Long,
    val dueAtAfterEpochMillis: Long,
)
