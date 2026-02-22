package com.cerebus.readwrite.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.cerebus.data.decks.data.dao.DeckDao
import com.cerebus.data.decks.data.entity.DeckEntity
import com.cerebus.data.flashcards.data.dao.FlashcardDao
import com.cerebus.data.flashcards.data.entity.FlashcardEntity
import com.cerebus.data.student.data.dao.StudentDao
import com.cerebus.data.student.data.entity.StudentEntity
import com.cerebus.data.studentdeck.data.dao.StudentDeckDao
import com.cerebus.data.studentdeck.data.entity.StudentDeckCrossRef

@Database(
    entities = [FlashcardEntity::class, DeckEntity::class, StudentEntity::class, StudentDeckCrossRef::class],
    version = 1,
)
@ConstructedBy(ReadWriteDatabaseConstructor::class)
abstract class ReadWriteDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun deckDao(): DeckDao
    abstract fun studentDao(): StudentDao
    abstract fun studentDeckDao(): StudentDeckDao
}

@Suppress("KotlinNoActualForExpect")
expect object ReadWriteDatabaseConstructor : RoomDatabaseConstructor<ReadWriteDatabase> {
    override fun initialize(): ReadWriteDatabase
}
