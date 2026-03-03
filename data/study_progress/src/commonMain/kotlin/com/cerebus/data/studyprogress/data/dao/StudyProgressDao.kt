package com.cerebus.data.studyprogress.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity

@Dao
interface StudyProgressDao {
    @Query(
        """
        SELECT * FROM study_progress
        WHERE user_id = :userId AND due_at <= :currentTimeMillis
        ORDER BY due_at ASC
        """
    )
    suspend fun getDueCardsForUser(
        userId: String,
        currentTimeMillis: Long,
    ): List<StudyProgressEntity>

    @Query(
        """
        SELECT * FROM study_progress
        WHERE user_id = :userId
        ORDER BY due_at ASC
        """
    )
    suspend fun getAllCardsForUser(userId: String): List<StudyProgressEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(studyProgress: StudyProgressEntity): Long

    @Query(
        """
        UPDATE study_progress
        SET
            state = :state,
            due_at = :dueAt,
            interval = :interval,
            ease = :ease,
            repetitions = :repetitions
        WHERE user_id = :userId AND card_id = :cardId
        """
    )
    suspend fun updateCardData(
        userId: String,
        cardId: String,
        state: Int,
        dueAt: Long,
        interval: Long,
        ease: Int,
        repetitions: Int,
    ): Int
}
