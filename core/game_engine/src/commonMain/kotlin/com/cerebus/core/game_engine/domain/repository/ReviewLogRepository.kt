package com.cerebus.core.game_engine.domain.repository

import com.cerebus.core.game_engine.domain.model.ReviewLog

interface ReviewLogRepository {
    suspend fun insertLog(log: ReviewLog)

    suspend fun getCardIdsFirstReviewedSince(
        studentId: String,
        sinceEpochMillis: Long,
    ): Set<String>
}
