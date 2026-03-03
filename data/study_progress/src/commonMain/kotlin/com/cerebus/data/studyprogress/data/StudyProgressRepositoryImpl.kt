package com.cerebus.data.studyprogress.data

import com.cerebus.data.studyprogress.data.mapper.toDomain
import com.cerebus.data.studyprogress.data.mapper.toEntity
import com.cerebus.data.studyprogress.data.storage.StudyProgressStorage
import com.cerebus.data.studyprogress.domain.models.StudyProgress
import com.cerebus.data.studyprogress.domain.models.StudyProgressUpdate
import com.cerebus.data.studyprogress.domain.repositories.StudyProgressRepository

class StudyProgressRepositoryImpl(
    private val storage: StudyProgressStorage,
) : StudyProgressRepository {
    override suspend fun getDueCardsForUser(
        userId: String,
        currentTimeMillis: Long,
    ): List<StudyProgress> {
        return storage
            .getDueCardsForUser(userId, currentTimeMillis)
            .map { it.toDomain() }
    }

    override suspend fun getAllCardsForUser(userId: String): List<StudyProgress> {
        return storage
            .getAllCardsForUser(userId)
            .map { it.toDomain() }
    }

    override suspend fun addStudyProgress(studyProgress: StudyProgress): Boolean {
        return storage.insert(studyProgress.toEntity())
    }

    override suspend fun updateCardData(
        userId: String,
        cardId: String,
        update: StudyProgressUpdate,
    ): Boolean {
        return storage.updateCardData(
            userId = userId,
            cardId = cardId,
            state = update.state.dbValue,
            dueAt = update.dueAt,
            interval = update.interval,
            ease = update.ease.dbValue,
            repetitions = update.repetitions,
        )
    }
}
