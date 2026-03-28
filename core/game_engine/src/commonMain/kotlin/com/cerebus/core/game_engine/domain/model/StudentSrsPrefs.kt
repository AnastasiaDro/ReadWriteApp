package com.cerebus.core.game_engine.domain.model

data class StudentSrsPrefs(
    val studentId: String,
    val newCardsPerSession: Int = 5,
    val reviewsPerSession: Int = 10,
    val learnMoreStep: Int = 5,
    val maxNewCardsPerDay: Int = 5,
    val allowNearMatch: Boolean = true,
    val similarityThreshold: Double = 0.85,
    val easyStreakRequired: Int = 2,
    val guidedHintSuccessThreshold: Int = 1,
    val updatedAtEpochMillis: Long = 0L,
)
