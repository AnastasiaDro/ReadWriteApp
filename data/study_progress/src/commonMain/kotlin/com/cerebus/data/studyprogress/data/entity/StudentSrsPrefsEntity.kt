package com.cerebus.data.studyprogress.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cerebus.data.student.data.entity.StudentEntity

@Entity(
    tableName = "student_srs_prefs",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["student_id"], unique = true),
    ],
)
data class StudentSrsPrefsEntity(
    @PrimaryKey
    @ColumnInfo(name = "student_id")
    val studentId: String,
    @ColumnInfo(name = "new_cards_per_session")
    val newCardsPerSession: Int,
    @ColumnInfo(name = "reviews_per_session")
    val reviewsPerSession: Int,
    @ColumnInfo(name = "learn_more_step")
    val learnMoreStep: Int,
    @ColumnInfo(name = "max_new_cards_per_day")
    val maxNewCardsPerDay: Int,
    @ColumnInfo(name = "allow_near_match")
    val allowNearMatch: Boolean,
    @ColumnInfo(name = "similarity_threshold")
    val similarityThreshold: Double,
    @ColumnInfo(name = "easy_streak_required")
    val easyStreakRequired: Int,
    @ColumnInfo(name = "guided_hint_success_threshold")
    val guidedHintSuccessThreshold: Int,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)
