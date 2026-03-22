package com.cerebus.data.studyprogress.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cerebus.data.studyprogress.data.entity.ReviewLogEntity
import com.cerebus.data.studyprogress.data.entity.StudentSrsPrefsEntity
import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyProgressDao {
    @Query(
        """
        SELECT * FROM study_progress
        WHERE student_id = :studentId AND card_id = :cardId
        LIMIT 1
        """
    )
    suspend fun getProgress(
        studentId: String,
        cardId: String,
    ): StudyProgressEntity?

    @Query(
        """
        SELECT * FROM study_progress
        WHERE student_id = :studentId
        """
    )
    fun observeProgressByStudent(studentId: String): Flow<List<StudyProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: StudyProgressEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReviewLog(log: ReviewLogEntity)

    @Query(
        """
        SELECT DISTINCT rl.card_id
        FROM review_log AS rl
        WHERE rl.student_id = :studentId
          AND rl.submitted_at_epoch_millis >= :sinceEpochMillis
          AND NOT EXISTS (
              SELECT 1
              FROM review_log AS prev
              WHERE prev.student_id = rl.student_id
                AND prev.card_id = rl.card_id
                AND prev.submitted_at_epoch_millis < :sinceEpochMillis
          )
        """
    )
    suspend fun getCardIdsFirstReviewedSince(
        studentId: String,
        sinceEpochMillis: Long,
    ): List<String>

    @Query(
        """
        SELECT * FROM student_srs_prefs
        WHERE student_id = :studentId
        LIMIT 1
        """
    )
    suspend fun getStudentPrefs(studentId: String): StudentSrsPrefsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudentPrefs(prefs: StudentSrsPrefsEntity)

    @Transaction
    suspend fun upsertProgressAndInsertLog(
        progress: StudyProgressEntity,
        log: ReviewLogEntity,
    ) {
        upsertProgress(progress)
        insertReviewLog(log)
    }
}
