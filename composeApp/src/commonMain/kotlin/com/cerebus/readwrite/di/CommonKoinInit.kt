package com.cerebus.readwrite.di

import com.cerebus.readwrite.di.core.databaseModule
import com.cerebus.readwrite.di.feature.flashcardsModule
import com.cerebus.readwrite.di.feature.gameScreenModule
import org.koin.core.module.Module

val modules = listOf<Module>(databaseModule, gameScreenModule, flashcardsModule)
expect fun initKoin(modules: List<Module>)
