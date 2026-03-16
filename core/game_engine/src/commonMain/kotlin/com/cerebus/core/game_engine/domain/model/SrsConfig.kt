package com.cerebus.core.game_engine.domain.model

data class SrsConfig(
    val levelIntervalsMillis: List<Long> = listOf(
        10_000L,
        30_000L,
        2 * 60_000L,
        10 * 60_000L,
        24 * 60 * 60_000L,
        3 * 24 * 60 * 60_000L,
    ),
    val requiredRecallSuccesses: Int = 2,
    val learnedLevelThreshold: Int = 4,
    val maxLevel: Int = 5,
)
