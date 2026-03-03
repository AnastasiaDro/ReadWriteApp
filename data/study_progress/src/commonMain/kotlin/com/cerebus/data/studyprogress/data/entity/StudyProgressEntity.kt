package com.cerebus.data.studyprogress.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.cerebus.data.flashcards.data.entity.FlashcardEntity
import com.cerebus.data.student.data.entity.StudentEntity

@Entity(
    tableName = "study_progress",
    primaryKeys = ["user_id", "card_id"],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
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
        Index(value = ["user_id", "due_at"]),
        Index(value = ["card_id"]),
    ],
)
data class StudyProgressEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    val state: Int,
    @ColumnInfo(name = "due_at")
    val dueAt: Long,
    val interval: Long,
    val ease: Int,
    val repetitions: Int,
)
