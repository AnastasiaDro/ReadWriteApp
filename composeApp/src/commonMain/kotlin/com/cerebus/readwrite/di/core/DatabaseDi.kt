package com.cerebus.readwrite.di.core

import com.cerebus.flashcards.data.dao.FlashcardDao
import com.cerebus.readwrite.data.ReadWriteDatabase
import org.koin.dsl.module

val databaseModule = module {
    single<ReadWriteDatabase> { createDatabase() }
    single<FlashcardDao> { get<ReadWriteDatabase>().flashcardDao() }
}
