package com.cerebus.readwrite

import com.cerebus.create_screen.navigation.CreateNavigationState
import com.cerebus.readwrite.navigation.StudentImportNavigationState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

fun handleIncomingArchiveUrl(url: String) {
    val sourceUrl = NSURL.URLWithString(url) ?: return
    when (sourceUrl.pathExtension?.lowercase().orEmpty()) {
        "rwdeck" -> {
            val preparedArchiveUrl = prepareIncomingArchiveUrl(
                url = url,
                targetExtension = "rwdeck",
            ) ?: return
            CreateNavigationState.requestImportDeckArchive(preparedArchiveUrl)
        }
        "rwstudent" -> {
            val preparedArchiveUrl = prepareIncomingArchiveUrl(
                url = url,
                targetExtension = "rwstudent",
            ) ?: return
            StudentImportNavigationState.requestImportStudentArchive(preparedArchiveUrl)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun prepareIncomingArchiveUrl(
    url: String,
    targetExtension: String,
): String? {
    val sourceUrl = NSURL.URLWithString(url) ?: return null
    val extension = sourceUrl.pathExtension?.lowercase().orEmpty()
    if (extension != targetExtension) return null

    val hasSecurityScope = sourceUrl.startAccessingSecurityScopedResource()
    try {
        val tempPath = normalizePath("${NSTemporaryDirectory()}/${targetExtension}_open_${NSUUID().UUIDString}.$targetExtension")
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
