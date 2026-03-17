package com.cerebus.readwrite.di.feature

import com.cerebus.create_screen.presentation.CreateScreenViewModel
import com.cerebus.create_screen.presentation.DeckGalleryViewModel
import com.cerebus.create_screen.presentation.DeckScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val createScreenModule = module {
    viewModelOf(::CreateScreenViewModel)
    viewModelOf(::DeckScreenViewModel)
    viewModel { (deckId: String, initialCardId: String?) ->
        DeckGalleryViewModel(
            deckId = deckId,
            initialCardId = initialCardId,
            deckRepository = get(),
            flashcardRepository = get(),
            preferencesRepository = get(),
            studentRepository = get(),
            studentPrefsRepository = get(),
        )
    }
}
