package com.cerebus.data.studyprogress.data.mapper

import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity
import com.cerebus.data.studyprogress.domain.models.StudyProgress
import com.cerebus.data.studyprogress.domain.models.EaseLevel
import com.cerebus.data.studyprogress.domain.models.StudyState

fun StudyProgressEntity.toDomain(): StudyProgress {
    return StudyProgress(
        userId = userId,
        cardId = cardId,
        state = StudyState.fromDbValue(state),
        dueAt = dueAt,
        interval = interval,
        ease = EaseLevel.fromDbValue(ease),
        repetitions = repetitions,
    )
}

fun StudyProgress.toEntity(): StudyProgressEntity {
    return StudyProgressEntity(
        userId = userId,
        cardId = cardId,
        state = state.dbValue,
        dueAt = dueAt,
        interval = interval,
        ease = ease.dbValue,
        repetitions = repetitions,
    )
}
