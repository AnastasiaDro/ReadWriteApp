package com.cerebus.readwrite.di.feature

import com.cerebus.data.preferences.data.PreferencesRepositoryImpl
import com.cerebus.data.preferences.data.storage.PreferencesStorage
import com.cerebus.data.preferences.data.storage.PreferencesStorageImpl
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import org.koin.dsl.module

val preferencesModule = module {
    single<PreferencesStorage> {
        PreferencesStorageImpl()
    }

    single<PreferencesRepository> {
        PreferencesRepositoryImpl(storage = get())
    }
}
