package com.cerebus.readwrite

import com.cerebus.create_screen.navigation.CreateNavigationState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

fun handleIncomingDeckArchiveUrl(url: String) {
    val preparedArchiveUrl = prepareIncomingDeckArchiveUrl(url) ?: return
    CreateNavigationState.requestImportDeckArchive(preparedArchiveUrl)
}

@OptIn(ExperimentalForeignApi::class)
private fun prepareIncomingDeckArchiveUrl(url: String): String? {
    val sourceUrl = NSURL.URLWithString(url) ?: return null
    val extension = sourceUrl.pathExtension?.lowercase().orEmpty()
    if (extension != "rwdeck") return null

    val hasSecurityScope = sourceUrl.startAccessingSecurityScopedResource()
    try {
        val tempPath = normalizePath("${NSTemporaryDirectory()}/rwdeck_open_${NSUUID().UUIDString}.rwdeck")
        val tempUrl = NSURL.fileURLWithPath(tempPath)
        NSFileManager.defaultManager.removeItemAtURL(tempUrl, error = null)
        val copied = NSFileManager.defaultManager.copyItemAtURL(
            srcURL = sourceUrl,
            toURL = tempUrl,
            error = null,
        )
        if (!copied) return null
        return tempUrl.absoluteString
    } finally {
        if (hasSecurityScope) {
            sourceUrl.stopAccessingSecurityScopedResource()
        }
    }
}

private fun normalizePath(path: String): String {
    val sanitized = path.replace('\\', '/')
    val isAbsolute = sanitized.startsWith("/")
    val segments = mutableListOf<String>()
    sanitized.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments += segment
        }
    }
    val normalized = segments.joinToString("/")
    return if (isAbsolute) "/$normalized" else normalized
}
