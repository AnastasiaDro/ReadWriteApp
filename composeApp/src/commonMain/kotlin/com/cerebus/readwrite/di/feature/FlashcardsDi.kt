package com.cerebus.readwrite.di.feature

import com.cerebus.flashcards.data.FlashcardRepositoryImpl
import com.cerebus.flashcards.data.storage.FlashcardStorage
import com.cerebus.flashcards.data.storage.FlashcardStorageImpl
import com.cerebus.flashcards.domain.repositories.FlashcardRepository
import org.koin.dsl.module

val flashcardsModule = module {

    /** Data **/
    /** Repository **/
    single<FlashcardRepository> {
        FlashcardRepositoryImpl(storage = get())
    }

    /** Storage **/
    single<FlashcardStorage> {
        FlashcardStorageImpl()
    }
}