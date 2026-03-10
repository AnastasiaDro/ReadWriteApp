package com.cerebus.readwrite.di

import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.readwrite.deckpackage.AndroidDeckPackageService
import com.cerebus.readwrite.MyApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module


val androidModules = listOf<Module>(
    module {
        single<DeckPackageService> {
            AndroidDeckPackageService(
                appContext = androidContext(),
                deckRepository = get(),
                flashcardRepository = get(),
                studentDeckRepository = get(),
            )
        }
    }
)
actual fun initKoin(modules: List<Module>) {
    startKoin {
        androidContext(MyApp.instance) // нужен Application контекст
        modules(*(modules + androidModules).toTypedArray())
    }
}
