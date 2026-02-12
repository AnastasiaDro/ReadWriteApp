package com.cerebus.readwrite.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

val iosModules = listOf<Module>()
actual fun initKoin(modules: List<Module>) {
    startKoin {
        modules(*(modules + iosModules).toTypedArray())
    }
}