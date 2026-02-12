package com.cerebus.readwrite.di

import com.cerebus.readwrite.MyApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module


val androidModules = listOf<Module>()
actual fun initKoin(modules: List<Module>) {
    startKoin {
        androidContext(MyApp.instance) // нужен Application контекст
        modules(*(modules + androidModules).toTypedArray())
    }
}