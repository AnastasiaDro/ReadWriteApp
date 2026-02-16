package com.cerebus.readwrite.di.feature

import com.cerebus.create_screen.presentation.CreateScreenViewModel
import com.cerebus.create_screen.presentation.DeckScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val createScreenModule = module {
    viewModelOf(::CreateScreenViewModel)
    viewModelOf(::DeckScreenViewModel)
}
