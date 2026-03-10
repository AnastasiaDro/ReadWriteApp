package com.cerebus.readwrite.media

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformMessenger(): PlatformMessenger {
    val context = LocalContext.current
    return remember(context) {
        object : PlatformMessenger {
            override fun showMessage(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
