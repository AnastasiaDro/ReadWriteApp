package com.cerebus.data.studyprogress.data.storage

import com.cerebus.data.studyprogress.data.dao.StudyProgressDao
import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity

class StudyProgressStorageImpl(
    private val dao: StudyProgressDao,
) : StudyProgressStorage {
    override suspend fun getDueCardsForUser(
        userId: String,
        currentTimeMillis: Long,
    ): List<StudyProgressEntity> {
        return runCatching {
            dao.getDueCardsForUser(userId, currentTimeMillis)
        }.getOrDefault(emptyList())
    }

    override suspend fun getAllCardsForUser(userId: String): List<StudyProgressEntity> {
        return runCatching {
            dao.getAllCardsForUser(userId)
        }.getOrDefault(emptyList())
    }

    override suspend fun insert(studyProgress: StudyProgressEntity): Boolean {
        return runCatching {
            dao.insert(studyProgress)
            true
        }.getOrDefault(false)
    }

    override suspend fun updateCardData(
        userId: String,
        cardId: String,
        state: Int,
        dueAt: Long,
        interval: Long,
        ease: Int,
        repetitions: Int,
    ): Boolean {
        return runCatching {
            dao.updateCardData(
                userId = userId,
                cardId = cardId,
                state = state,
                dueAt = dueAt,
                interval = interval,
                ease = ease,
                repetitions = repetitions,
            ) > 0
        }.getOrDefault(false)
    }
}
