package com.cerebus.data.studyprogress.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.cerebus.data.flashcards.data.entity.FlashcardEntity
import com.cerebus.data.student.data.entity.StudentEntity

@Entity(
    tableName = "study_progress",
    primaryKeys = ["student_id", "card_id"],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["student_id", "due_at_epoch_millis"]),
        Index(value = ["card_id"]),
    ],
)
data class StudyProgressEntity(
    @ColumnInfo(name = "student_id")
    val studentId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    val state: Int,
    @ColumnInfo(name = "due_at_epoch_millis")
    val dueAtEpochMillis: Long,
    @ColumnInfo(name = "interval_days")
    val intervalDays: Double,
    val ease: Double,
    @ColumnInfo(name = "learning_step_index")
    val learningStepIndex: Int,
    val reps: Int,
    val lapses: Int,
    @ColumnInfo(name = "last_reviewed_at_epoch_millis")
    val lastReviewedAtEpochMillis: Long?,
    @ColumnInfo(name = "last_grade")
    val lastGrade: Int?,
)
