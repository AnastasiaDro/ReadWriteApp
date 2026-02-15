package com.cerebus.readwrite

import androidx.compose.ui.window.ComposeUIViewController
import com.cerebus.readwrite.di.initKoin
import com.cerebus.readwrite.di.modules

private var isKoinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!isKoinInitialized) {
        initKoin(modules)
        isKoinInitialized = true
    }
    ReadWriteAppNavigation()
}
