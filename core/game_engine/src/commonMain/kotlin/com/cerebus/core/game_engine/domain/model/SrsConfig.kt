package com.cerebus.core.game_engine.domain.model

data class SrsConfig(
    val learningStepsMillis: List<Long> = listOf(
        5 * 60_000L,
        30 * 60_000L,
        24 * 60 * 60_000L,
    ),
    val relearningStepsMillis: List<Long> = listOf(
        5 * 60_000L,
        30 * 60_000L,
    ),
    val graduatingIntervalDays: Double = 2.0,
    val easyIntervalDays: Double = 4.0,
    val easeStart: Double = 2.3,
    val easeMin: Double = 1.5,
    val easyBonus: Double = 1.15,
    val lapseNewIntervalFactor: Double = 0.5,
    val minIntervalDays: Double = 1.0,
    val maxIntervalDays: Double = 3650.0,
)
