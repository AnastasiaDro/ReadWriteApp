package com.cerebus.readwrite.di.feature

import com.cerebus.session_settings.presentation.SessionSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sessionSettingsModule = module {
    viewModel { (studentId: String) ->
        SessionSettingsViewModel(
            studentId = studentId,
            prefsRepository = get(),
            preferencesRepository = get(),
            deckRepository = get(),
            studentDeckRepository = get(),
        )
    }
}
