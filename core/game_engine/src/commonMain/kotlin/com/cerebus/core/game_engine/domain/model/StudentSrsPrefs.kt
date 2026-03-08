package com.cerebus.core.game_engine.domain.model

data class StudentSrsPrefs(
    val studentId: String,
    val newCardsPerSession: Int = 5,
    val reviewsPerSession: Int = 15,
    val learnMoreStep: Int = 5,
    val maxNewCardsPerDay: Int = 15,
    val allowNearMatch: Boolean = true,
    val similarityThreshold: Double = 0.85,
    val easyStreakRequired: Int = 2,
    val guidedHintSuccessThreshold: Int = 2,
)
