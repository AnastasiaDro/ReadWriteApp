package com.cerebus.readwrite.di.core

import com.cerebus.data.decks.data.dao.DeckDao
import com.cerebus.data.flashcards.data.dao.FlashcardDao
import com.cerebus.data.student.data.dao.StudentDao
import com.cerebus.data.studentdeck.data.dao.StudentDeckDao
import com.cerebus.readwrite.data.ReadWriteDatabase
import org.koin.dsl.module

val databaseModule = module {
    single<ReadWriteDatabase> { createDatabase() }
    single<FlashcardDao> { get<ReadWriteDatabase>().flashcardDao() }
    single<DeckDao> { get<ReadWriteDatabase>().deckDao() }
    single<StudentDao> { get<ReadWriteDatabase>().studentDao() }
    single<StudentDeckDao> { get<ReadWriteDatabase>().studentDeckDao() }
}
