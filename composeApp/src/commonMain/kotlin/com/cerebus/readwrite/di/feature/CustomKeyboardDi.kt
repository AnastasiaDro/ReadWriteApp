package com.cerebus.readwrite.di.feature

import com.cerebus.customkeyboard.KeyboardSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val customKeyboardModule = module {
    viewModel { (studentId: String) ->
        KeyboardSettingsViewModel(
            studentId = studentId,
            studentRepository = get(),
            preferencesRepository = get(),
        )
    }
}
