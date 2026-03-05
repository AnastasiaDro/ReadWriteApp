package com.cerebus.core.game_engine.domain.repository

import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.ReviewLog

interface ReviewLogRepository {
    suspend fun insertLog(log: ReviewLog)

    suspend fun getRecentGrades(
        studentId: String,
        cardId: String,
        limit: Int,
    ): List<Grade>
}
