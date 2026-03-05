package com.cerebus.core.game_engine.domain.model

data class SchedulerQueues(
    val dueLearning: List<String>,
    val dueRelearning: List<String>,
    val dueReview: List<String>,
    val newCards: List<String>,
)
