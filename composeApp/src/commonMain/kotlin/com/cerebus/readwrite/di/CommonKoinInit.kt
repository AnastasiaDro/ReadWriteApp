package com.cerebus.readwrite.di

import org.koin.core.module.Module


val modules = listOf<Module>()
expect fun initKoin(modules: List<Module>)

