package com.cerebus.readwrite.di

import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.readwrite.deckpackage.IosDeckPackageService
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val iosModules = listOf<Module>(
    module {
        single<DeckPackageService> {
            IosDeckPackageService(
                deckRepository = get(),
                flashcardRepository = get(),
                studentDeckRepository = get(),
            )
        }
    }
)
actual fun initKoin(modules: List<Module>) {
    startKoin {
        modules(*(modules + iosModules).toTypedArray())
    }
}
