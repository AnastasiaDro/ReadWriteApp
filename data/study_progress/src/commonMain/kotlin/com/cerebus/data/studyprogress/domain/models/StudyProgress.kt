package com.cerebus.data.studyprogress.domain.models

data class StudyProgress(
    val userId: String,
    val cardId: String,
    val state: StudyState,
    val dueAt: Long,
    val interval: Long,
    val ease: EaseLevel,
    val repetitions: Int,
)
