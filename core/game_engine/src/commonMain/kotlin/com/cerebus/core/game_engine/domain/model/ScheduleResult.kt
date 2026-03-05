package com.cerebus.core.game_engine.domain.model

data class ScheduleResult(
    val updatedProgress: CardProgress,
    val dueAtEpochMillis: Long,
)
