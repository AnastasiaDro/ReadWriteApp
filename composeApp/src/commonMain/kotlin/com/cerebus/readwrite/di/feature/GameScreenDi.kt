package com.cerebus.readwrite.di.feature

import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerAndRescheduleUseCase
import com.cerebus.game_screen.presentation.GameScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val gameScreenModule = module {
    single {
        SubmitAnswerAndRescheduleUseCase(
            progressRepository = get(),
            studentPrefsRepository = get(),
            reviewLogRepository = get(),
        )
    }

    viewModel { (deckIds: List<String>) ->
        GameScreenViewModel(
            deckIds = deckIds,
            flashcardRepository = get(),
            deckRepository = get(),
            preferencesRepository = get(),
            submitAnswerAndRescheduleUseCase = get(),
        )
    }
}
