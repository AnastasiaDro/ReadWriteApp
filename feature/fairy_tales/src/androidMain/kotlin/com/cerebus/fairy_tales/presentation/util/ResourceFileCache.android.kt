package com.cerebus.fairy_tales.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
actual fun rememberResourceFileCache(): ResourceFileCache {
    val context = LocalContext.current
    return remember(context) {
        object : ResourceFileCache {
            private val cacheRoot = File(context.cacheDir, "fairy_tales_media").apply { mkdirs() }

            override fun cacheBytes(
                fileName: String,
                bytes: ByteArray,
            ): String? = runCatching {
                val target = File(cacheRoot, fileName)
                target.writeBytes(bytes)
                target.absolutePath
            }.getOrNull()
        }
    }
}
