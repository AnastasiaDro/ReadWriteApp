package com.cerebus.readwrite.di.feature

import com.cerebus.game_screen.presentation.GameScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val gameScreenModule = module {
    viewModel { (deckId: String) ->
        GameScreenViewModel(
            deckId = deckId,
            flashcardRepository = get(),
            deckRepository = get(),
        )
    }
}
