package com.cerebus.readwrite.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.cerebus.decks.data.dao.DeckDao
import com.cerebus.decks.data.entity.DeckEntity
import com.cerebus.flashcards.data.dao.FlashcardDao
import com.cerebus.flashcards.data.entity.FlashcardEntity

@Database(
    entities = [FlashcardEntity::class, DeckEntity::class],
    version = 2,
)
@ConstructedBy(ReadWriteDatabaseConstructor::class)
abstract class ReadWriteDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun deckDao(): DeckDao
}

@Suppress("KotlinNoActualForExpect")
expect object ReadWriteDatabaseConstructor : RoomDatabaseConstructor<ReadWriteDatabase> {
    override fun initialize(): ReadWriteDatabase
}
