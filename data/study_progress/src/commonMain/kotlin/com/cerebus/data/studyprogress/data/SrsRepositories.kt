package com.cerebus.data.studyprogress.data

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.ReviewLog
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.core.game_engine.domain.repository.AtomicProgressLogRepository
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.data.studyprogress.data.dao.StudyProgressDao
import com.cerebus.data.studyprogress.data.mapper.toDomain
import com.cerebus.data.studyprogress.data.mapper.toEntity
import com.cerebus.data.studyprogress.data.mapper.toGrade
import kotlinx.coroutines.flow.map

class RoomCardProgressRepository(
    private val dao: StudyProgressDao,
) : CardProgressRepository, AtomicProgressLogRepository {
    override suspend fun getProgress(
        studentId: String,
        cardId: String,
    ): CardProgress? {
        return dao.getProgress(studentId = studentId, cardId = cardId)?.toDomain()
    }

    override fun observeProgress(studentId: String) = dao.observeProgressByStudent(studentId)
        .map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertProgress(progress: CardProgress) {
        dao.upsertProgress(progress.toEntity())
    }

    override suspend fun upsertProgressAndInsertLog(
        progress: CardProgress,
        log: ReviewLog,
    ) {
        dao.upsertProgressAndInsertLog(
            progress = progress.toEntity(),
            log = log.toEntity(),
        )
    }
}

class RoomReviewLogRepository(
    private val dao: StudyProgressDao,
) : ReviewLogRepository {
    override suspend fun insertLog(log: ReviewLog) {
        dao.insertReviewLog(log.toEntity())
    }

    override suspend fun getRecentGrades(
        studentId: String,
        cardId: String,
        limit: Int,
    ): List<Grade> {
        if (limit <= 0) return emptyList()
        return dao.getRecentGrades(
            studentId = studentId,
            cardId = cardId,
            limit = limit,
        ).map { it.toGrade() }
    }
}

class RoomStudentPrefsRepository(
    private val dao: StudyProgressDao,
) : StudentPrefsRepository {
    override suspend fun getPrefs(studentId: String): StudentSrsPrefs {
        val stored = dao.getStudentPrefs(studentId)
        return stored?.toDomain() ?: StudentSrsPrefs(studentId = studentId)
    }

    override suspend fun savePrefs(prefs: StudentSrsPrefs) {
        dao.upsertStudentPrefs(prefs.toEntity())
    }
}
