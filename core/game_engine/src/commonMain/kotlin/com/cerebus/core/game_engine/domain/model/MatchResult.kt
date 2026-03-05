package com.cerebus.core.game_engine.domain.model

data class MatchResult(
    val userNorm: String,
    val bestExpectedNorm: String,
    val similarity: Double,
    val isExact: Boolean,
    val matchType: MatchType,
)
