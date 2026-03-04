package com.cerebus.data.studyprogress.domain.repositories

import com.cerebus.core.utils.nowMillis
import com.cerebus.data.studyprogress.domain.models.StudyProgress
import com.cerebus.data.studyprogress.domain.models.StudyProgressUpdate

interface StudyProgressRepository {
    suspend fun getDueCardsForUser(
        userId: String,
        currentTimeMillis: Long = nowMillis(),
    ): List<StudyProgress>

    suspend fun getAllCardsForUser(userId: String): List<StudyProgress>

    suspend fun addStudyProgress(studyProgress: StudyProgress): Boolean

    suspend fun updateCardData(
        userId: String,
        cardId: String,
        update: StudyProgressUpdate,
    ): Boolean
}
