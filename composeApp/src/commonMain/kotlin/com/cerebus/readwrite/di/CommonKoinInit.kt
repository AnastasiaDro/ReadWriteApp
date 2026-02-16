package com.cerebus.readwrite.di

import com.cerebus.readwrite.di.core.databaseModule
import com.cerebus.readwrite.di.feature.createScreenModule
import com.cerebus.readwrite.di.feature.decksModule
import com.cerebus.readwrite.di.feature.flashcardsModule
import com.cerebus.readwrite.di.feature.gameScreenModule
import org.koin.core.module.Module

val modules = listOf<Module>(
    databaseModule,
    gameScreenModule,
    flashcardsModule,
    decksModule,
    createScreenModule,
)
expect fun initKoin(modules: List<Module>)
