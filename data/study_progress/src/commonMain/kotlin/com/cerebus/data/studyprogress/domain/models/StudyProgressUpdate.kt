package com.cerebus.data.studyprogress.domain.models

data class StudyProgressUpdate(
    val state: StudyState,
    val dueAt: Long,
    val interval: Long,
    val ease: EaseLevel,
    val repetitions: Int,
)
