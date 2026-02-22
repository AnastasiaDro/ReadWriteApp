package com.cerebus.readwrite.di.feature

import com.cerebus.data.flashcards.data.FlashcardRepositoryImpl
import com.cerebus.data.flashcards.data.storage.FlashcardStorage
import com.cerebus.data.flashcards.data.storage.FlashcardStorageImpl
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import org.koin.dsl.module

val flashcardsModule = module {
    single<FlashcardRepository> {
        FlashcardRepositoryImpl(storage = get())
    }

    single<FlashcardStorage> {
        FlashcardStorageImpl(dao = get())
    }
}
