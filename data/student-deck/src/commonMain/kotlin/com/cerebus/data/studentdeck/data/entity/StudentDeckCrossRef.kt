package com.cerebus.data.studentdeck.data.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["studentId", "deckId"],
    indices = [Index("deckId")],
)
data class StudentDeckCrossRef(
    val studentId: String,
    val deckId: String,
)
