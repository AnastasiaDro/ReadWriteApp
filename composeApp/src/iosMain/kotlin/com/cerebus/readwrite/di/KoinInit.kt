package com.cerebus.readwrite.di

import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.deck_package.domain.service.StudentPackageService
import com.cerebus.readwrite.deckpackage.IosDeckPackageService
import com.cerebus.readwrite.deckpackage.IosStudentPackageService
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
        single<StudentPackageService> {
            IosStudentPackageService(
                studentRepository = get(),
                studentDeckRepository = get(),
                deckRepository = get(),
                flashcardRepository = get(),
                cardProgressRepository = get(),
                reviewLogRepository = get(),
                studentPrefsRepository = get(),
            )
        }
    }
)
actual fun initKoin(modules: List<Module>) {
    startKoin {
        modules(*(modules + iosModules).toTypedArray())
    }
}
