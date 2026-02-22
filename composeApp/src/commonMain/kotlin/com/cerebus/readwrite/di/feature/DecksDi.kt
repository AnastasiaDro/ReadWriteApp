package com.cerebus.readwrite.di.feature

import com.cerebus.data.decks.data.DeckRepositoryImpl
import com.cerebus.data.decks.data.storage.DeckStorage
import com.cerebus.data.decks.data.storage.DeckStorageImpl
import com.cerebus.data.decks.domain.repositories.DeckRepository
import org.koin.dsl.module

val decksModule = module {
    single<DeckRepository> {
        DeckRepositoryImpl(storage = get())
    }

    single<DeckStorage> {
        DeckStorageImpl(dao = get())
    }
}
