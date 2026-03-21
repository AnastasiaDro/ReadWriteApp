package com.cerebus.fairy_tales.presentation.util

import androidx.compose.runtime.Composable

interface PlatformMessenger {
    fun showMessage(message: String)
}

@Composable
expect fun rememberPlatformMessenger(): PlatformMessenger
