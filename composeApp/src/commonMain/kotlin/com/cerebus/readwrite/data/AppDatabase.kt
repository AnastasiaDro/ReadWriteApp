package com.cerebus.readwrite.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.cerebus.flashcards.data.dao.FlashcardDao
import com.cerebus.flashcards.data.entity.FlashcardEntity

@Database(
    entities = [FlashcardEntity::class],
    version = 1,
)
@ConstructedBy(ReadWriteDatabaseConstructor::class)
abstract class ReadWriteDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
}

@Suppress("KotlinNoActualForExpect")
expect object ReadWriteDatabaseConstructor : RoomDatabaseConstructor<ReadWriteDatabase> {
    override fun initialize(): ReadWriteDatabase
}
