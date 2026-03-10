package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable

interface PlatformMessenger {
    fun showMessage(message: String)
}

@Composable
expect fun rememberPlatformMessenger(): PlatformMessenger
