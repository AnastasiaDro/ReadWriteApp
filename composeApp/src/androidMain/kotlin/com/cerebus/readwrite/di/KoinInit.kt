package com.cerebus.readwrite.di

import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.deck_package.domain.service.StudentPackageService
import com.cerebus.readwrite.deckpackage.AndroidDeckPackageService
import com.cerebus.readwrite.deckpackage.AndroidStudentPackageService
import com.cerebus.readwrite.MyApp
import com.cerebus.readwrite.student.AndroidStarterDeckInstaller
import com.cerebus.readwrite.view.StarterDeckInstaller
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
        single<StudentPackageService> {
            AndroidStudentPackageService(
                appContext = androidContext(),
                studentRepository = get(),
                studentDeckRepository = get(),
                deckRepository = get(),
                flashcardRepository = get(),
                cardProgressRepository = get(),
                reviewLogRepository = get(),
                studentPrefsRepository = get(),
            )
        }
        single<StarterDeckInstaller> {
            AndroidStarterDeckInstaller(
                appContext = androidContext(),
                deckPackageService = get(),
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
