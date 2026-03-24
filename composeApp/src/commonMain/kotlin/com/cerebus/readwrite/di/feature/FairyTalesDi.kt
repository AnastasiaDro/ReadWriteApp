package com.cerebus.readwrite.di.feature

import com.cerebus.fairy_tales.presentation.FairyTalesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val fairyTalesModule = module {
    viewModel { (fairyTaleId: String) ->
        FairyTalesViewModel(
            fairyTaleId = fairyTaleId,
            preferencesRepository = get(),
            studentRepository = get(),
        )
    }
}
