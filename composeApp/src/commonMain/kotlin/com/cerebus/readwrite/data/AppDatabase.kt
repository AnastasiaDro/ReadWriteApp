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
import com.cerebus.data.studyprogress.data.dao.StudyProgressDao
import com.cerebus.data.studyprogress.data.entity.ReviewLogEntity
import com.cerebus.data.studyprogress.data.entity.StudentSrsPrefsEntity
import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity

@Database(
    entities = [
        FlashcardEntity::class,
        DeckEntity::class,
        StudentEntity::class,
        StudentDeckCrossRef::class,
        StudyProgressEntity::class,
        ReviewLogEntity::class,
        StudentSrsPrefsEntity::class,
    ],
    version = 5,
)
@ConstructedBy(ReadWriteDatabaseConstructor::class)
abstract class ReadWriteDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun deckDao(): DeckDao
    abstract fun studentDao(): StudentDao
    abstract fun studentDeckDao(): StudentDeckDao
    abstract fun studyProgressDao(): StudyProgressDao
}

@Suppress("KotlinNoActualForExpect")
expect object ReadWriteDatabaseConstructor : RoomDatabaseConstructor<ReadWriteDatabase> {
    override fun initialize(): ReadWriteDatabase
}
