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
    @ColumnInfo(name = "best_expected_normalized")
    val bestExpectedNormalized: String,
    val similarity: Double,
    @ColumnInfo(name = "is_exact")
    val isExact: Boolean,
    @ColumnInfo(name = "match_type")
    val matchType: Int,
    @ColumnInfo(name = "used_hint")
    val usedHint: Boolean,
    @ColumnInfo(name = "attempt_index")
    val attemptIndex: Int,
    @ColumnInfo(name = "state_before")
    val stateBefore: Int,
    @ColumnInfo(name = "state_after")
    val stateAfter: Int,
    val grade: Int,
    @ColumnInfo(name = "scheduled_due_at_before_epoch_millis")
    val scheduledDueAtBeforeEpochMillis: Long,
    @ColumnInfo(name = "due_at_after_epoch_millis")
    val dueAtAfterEpochMillis: Long,
    @ColumnInfo(name = "interval_before_days")
    val intervalBeforeDays: Double,
    @ColumnInfo(name = "interval_after_days")
    val intervalAfterDays: Double,
    @ColumnInfo(name = "ease_before")
    val easeBefore: Double,
    @ColumnInfo(name = "ease_after")
    val easeAfter: Double,
)
