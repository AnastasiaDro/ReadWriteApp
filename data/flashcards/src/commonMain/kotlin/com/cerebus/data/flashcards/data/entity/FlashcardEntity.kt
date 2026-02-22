package com.cerebus.data.flashcards.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val name: String,
    val activeLetters: String,
    val deckId: String? = null,
)
