package com.cerebus.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

private val resolvedUriCache = mutableMapOf<String, String>()
private const val DOCUMENTS_PREFIX = "documents/"
private const val APP_SUPPORT_PREFIX = "appsupport/"

actual fun persistLocalFileUri(uri: String?): String? {
    if (uri.isNullOrBlank()) return uri

    val localPath = resolvePathFromUri(uri) ?: return uri

    relativizeAgainstRoot(
        localPath = localPath,
        rootPath = documentsDirectoryPath(),
        prefix = DOCUMENTS_PREFIX,
    )?.let { return it }

    applicationSupportDirectoryPath()?.let { appSupportPath ->
        relativizeAgainstRoot(
            localPath = localPath,
            rootPath = appSupportPath,
            prefix = APP_SUPPORT_PREFIX,
        )?.let { return it }
    }

    return uri
}

actual fun resolvePersistedLocalFileUri(uri: String?): String? {
    if (uri.isNullOrBlank()) return uri

    return resolvedUriCache.getOrPut(uri) {
        resolvePersistedLocalFileUriInternal(uri)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun resolvePersistedLocalFileUriInternal(uri: String): String {
    resolveRelativePersistedUri(uri)?.let { relativeUri ->
        return relativeUri
    }

    val localPath = resolvePathFromUri(uri) ?: return uri
    if (fileExists(localPath)) {
        return toFileUri(localPath)
    }

    relocateInsideCurrentContainer(localPath)?.let { repairedPath ->
        return toFileUri(repairedPath)
    }

    val fileName = localPath.substringAfterLast('/', missingDelimiterValue = "")
    if (fileName.isBlank()) return uri

    findFileByName(documentsDirectoryPath(), fileName)?.let { repairedPath ->
        return toFileUri(repairedPath)
    }
    applicationSupportDirectoryPath()?.let { appSupportPath ->
        findFileByName(appSupportPath, fileName)?.let { repairedPath ->
            return toFileUri(repairedPath)
        }
    }

    return uri
}

private fun resolveRelativePersistedUri(uri: String): String? {
    resolvePersistedRelativePath(uri)?.let { path ->
        if (fileExists(path)) {
            return toFileUri(path)
        }
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
private fun relocateInsideCurrentContainer(oldPath: String): String? {
    relocateRelativeToRoot(
        oldPath = oldPath,
        marker = "/Documents/",
        currentRoot = documentsDirectoryPath(),
    )?.let { return it }

    val applicationSupportPath = applicationSupportDirectoryPath()
    if (applicationSupportPath != null) {
        relocateRelativeToRoot(
            oldPath = oldPath,
            marker = "/Library/Application Support/",
            currentRoot = applicationSupportPath,
        )?.let { return it }
    }

    return null
}

private fun relativizeAgainstRoot(
    localPath: String,
    rootPath: String,
    prefix: String,
): String? {
    val normalizedRoot = normalizePath(rootPath).trimEnd('/')
    val normalizedPath = normalizePath(localPath)
    if (normalizedPath != normalizedRoot && !normalizedPath.startsWith("$normalizedRoot/")) {
        return null
    }

    val relativePath = normalizedPath.removePrefix(normalizedRoot).trimStart('/')
    if (relativePath.isBlank()) return prefix.removeSuffix("/")
    return prefix + relativePath
}

private fun resolvePersistedRelativePath(uri: String): String? {
    when {
        uri.startsWith(DOCUMENTS_PREFIX) -> {
            val relativePath = uri.removePrefix(DOCUMENTS_PREFIX)
            return normalizePath("${documentsDirectoryPath()}/$relativePath")
        }

        uri.startsWith(APP_SUPPORT_PREFIX) -> {
            val appSupportPath = applicationSupportDirectoryPath() ?: return null
            val relativePath = uri.removePrefix(APP_SUPPORT_PREFIX)
            return normalizePath("$appSupportPath/$relativePath")
        }
    }

    return null
}

@OptIn(ExperimentalForeignApi::class)
private fun relocateRelativeToRoot(
    oldPath: String,
    marker: String,
    currentRoot: String,
): String? {
    val markerIndex = oldPath.indexOf(marker)
    if (markerIndex < 0) return null

    val relativeSuffix = oldPath.substring(markerIndex + marker.length)
    val candidatePath = normalizePath("$currentRoot/$relativeSuffix")
    return candidatePath.takeIf(::fileExists)
}

@OptIn(ExperimentalForeignApi::class)
private fun findFileByName(
    rootPath: String,
    fileName: String,
): String? {
    if (!fileExists(rootPath)) return null

    val subpaths = NSFileManager.defaultManager.subpathsOfDirectoryAtPath(
        rootPath,
        error = null,
    ) as? List<*> ?: return null

    val relativeMatch = subpaths
        .mapNotNull { it as? String }
        .firstOrNull { relativePath ->
            relativePath == fileName || relativePath.endsWith("/$fileName")
        } ?: return null

    return normalizePath("$rootPath/$relativeMatch")
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectoryPath(): String {
    val documentDir = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).firstOrNull() as? String
    return normalizePath(requireNotNull(documentDir))
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectoryPath(): String? {
    val appSupportDir = NSSearchPathForDirectoriesInDomains(
        directory = NSApplicationSupportDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).firstOrNull() as? String
    return appSupportDir?.let(::normalizePath)
}

@OptIn(ExperimentalForeignApi::class)
private fun resolvePathFromUri(uri: String): String? {
    if (uri.isBlank()) return null
    if (uri.startsWith("/")) return normalizePath(uri)

    val parsed = NSURL.URLWithString(uri)
    if (parsed?.isFileURL() == true) {
        val path = parsed.path ?: return null
        return normalizePath(path)
    }

    if (uri.startsWith("file://")) {
        val rawPath = uri.removePrefix("file://")
        if (rawPath.startsWith("/")) return normalizePath(rawPath)
    }

    return null
}

private fun toFileUri(path: String): String {
    return NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
}

private fun fileExists(path: String): Boolean {
    return NSFileManager.defaultManager.fileExistsAtPath(path)
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
