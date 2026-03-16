package com.cerebus.data.studyprogress.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cerebus.data.flashcards.data.entity.FlashcardEntity
import com.cerebus.data.student.data.entity.StudentEntity

@Entity(
    tableName = "review_log",
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
        Index(value = ["student_id", "card_id", "submitted_at_epoch_millis"]),
        Index(value = ["card_id"]),
    ],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "student_id")
    val studentId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "shown_at_epoch_millis")
    val shownAtEpochMillis: Long,
    @ColumnInfo(name = "submitted_at_epoch_millis")
    val submittedAtEpochMillis: Long,
    @ColumnInfo(name = "user_input_raw")
    val userInputRaw: String,
    @ColumnInfo(name = "user_input_normalized")
    val userInputNormalized: String,
    @ColumnInfo(name = "expected_answer_normalized")
    val expectedAnswerNormalized: String,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    @ColumnInfo(name = "hint_level")
    val hintLevel: Int,
    @ColumnInfo(name = "wrong_press_count")
    val wrongPressCount: Int,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "copy_stage")
    val copyStage: Boolean,
    @ColumnInfo(name = "level_before")
    val levelBefore: Int,
    @ColumnInfo(name = "level_after")
    val levelAfter: Int,
    @ColumnInfo(name = "recall_success_streak_before")
    val recallSuccessStreakBefore: Int,
    @ColumnInfo(name = "recall_success_streak_after")
    val recallSuccessStreakAfter: Int,
    @ColumnInfo(name = "copy_success_streak_before")
    val copySuccessStreakBefore: Int,
    @ColumnInfo(name = "copy_success_streak_after")
    val copySuccessStreakAfter: Int,
    @ColumnInfo(name = "due_at_before_epoch_millis")
    val dueAtBeforeEpochMillis: Long,
    @ColumnInfo(name = "due_at_after_epoch_millis")
    val dueAtAfterEpochMillis: Long,
)
