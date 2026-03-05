package com.cerebus.core.game_engine.domain.repository

import com.cerebus.core.game_engine.domain.model.CardProgress
import kotlinx.coroutines.flow.Flow

interface CardProgressRepository {
    suspend fun getProgress(
        studentId: String,
        cardId: String,
    ): CardProgress?

    fun observeProgress(studentId: String): Flow<List<CardProgress>>

    suspend fun upsertProgress(progress: CardProgress)
}
