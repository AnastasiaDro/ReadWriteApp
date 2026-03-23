package com.cerebus.fairy_tales.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@Composable
actual fun rememberResourceFileCache(): ResourceFileCache =
    remember {
        IosResourceFileCache()
    }

@OptIn(ExperimentalForeignApi::class)
private class IosResourceFileCache : ResourceFileCache {
    override fun cacheBytes(
        fileName: String,
        bytes: ByteArray,
    ): String? = runCatching {
        val safeFileName = fileName.replace("/", "_")
        val directoryPath = "${NSTemporaryDirectory().trimEnd('/')}/fairy_tales_media"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = directoryPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val filePath = "$directoryPath/$safeFileName"
        val file = fopen(filePath, "wb") ?: return null
        try {
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fwrite(
                        pinned.addressOf(0),
                        1.convert(),
                        bytes.size.convert(),
                        file,
                    )
                }
            }
        } finally {
            fclose(file)
        }
        NSURL.fileURLWithPath(filePath).absoluteString
    }.getOrNull()
}
