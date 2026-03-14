package com.cerebus.readwrite.di

import com.cerebus.readwrite.di.core.databaseModule
import com.cerebus.readwrite.di.feature.createScreenModule
import com.cerebus.readwrite.di.feature.customKeyboardModule
import com.cerebus.readwrite.di.feature.decksModule
import com.cerebus.readwrite.di.feature.flashcardsModule
import com.cerebus.readwrite.di.feature.gameScreenModule
import com.cerebus.readwrite.di.feature.preferencesModule
import com.cerebus.readwrite.di.feature.sessionSettingsModule
import com.cerebus.readwrite.di.feature.studyProgressModule
import com.cerebus.readwrite.di.feature.studentDecksModule
import com.cerebus.readwrite.di.feature.studentFeatureModule
import com.cerebus.readwrite.di.feature.studentsModule
import org.koin.core.module.Module

val modules = listOf<Module>(
    databaseModule,
    gameScreenModule,
    flashcardsModule,
    decksModule,
    studentsModule,
    studentDecksModule,
    preferencesModule,
    studyProgressModule,
    sessionSettingsModule,
    customKeyboardModule,
    createScreenModule,
    studentFeatureModule,
)
expect fun initKoin(modules: List<Module>)
