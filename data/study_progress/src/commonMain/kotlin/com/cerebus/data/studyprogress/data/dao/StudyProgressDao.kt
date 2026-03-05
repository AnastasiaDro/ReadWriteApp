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
        SELECT grade FROM review_log
        WHERE student_id = :studentId AND card_id = :cardId
        ORDER BY submitted_at_epoch_millis DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentGrades(
        studentId: String,
        cardId: String,
        limit: Int,
    ): List<Int>

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
