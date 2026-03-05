package com.cerebus.core.game_engine.domain.repository

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.ReviewLog

interface AtomicProgressLogRepository {
    suspend fun upsertProgressAndInsertLog(
        progress: CardProgress,
        log: ReviewLog,
    )
}
