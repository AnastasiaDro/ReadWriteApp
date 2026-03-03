package com.cerebus.data.studyprogress.data.storage

import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity

interface StudyProgressStorage {
    suspend fun getDueCardsForUser(
        userId: String,
        currentTimeMillis: Long,
    ): List<StudyProgressEntity>

    suspend fun getAllCardsForUser(userId: String): List<StudyProgressEntity>

    suspend fun insert(studyProgress: StudyProgressEntity): Boolean

    suspend fun updateCardData(
        userId: String,
        cardId: String,
        state: Int,
        dueAt: Long,
        interval: Long,
        ease: Int,
        repetitions: Int,
    ): Boolean
}
