package com.cerebus.fairy_tales.presentation.util

import androidx.compose.runtime.Composable

interface ResourceFileCache {
    fun cacheBytes(
        fileName: String,
        bytes: ByteArray,
    ): String?
}

@Composable
expect fun rememberResourceFileCache(): ResourceFileCache
