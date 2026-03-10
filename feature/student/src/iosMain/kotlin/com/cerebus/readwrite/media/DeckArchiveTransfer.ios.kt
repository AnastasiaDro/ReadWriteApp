package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject

@Composable
actual fun rememberDeckArchivePicker(
    onArchivePicked: (String) -> Unit,
    onError: (String) -> Unit,
): DeckArchivePicker {
    val onArchivePickedState = rememberUpdatedState(onArchivePicked)
    val onErrorState = rememberUpdatedState(onError)
    val delegate = remember {
        IOSDeckArchivePickerDelegate(
            onArchivePicked = { uri -> onArchivePickedState.value(uri) },
            onError = { message -> onErrorState.value(message) },
        )
    }

    return remember {
        object : DeckArchivePicker {
            override fun openArchivePicker() {
                openArchivePicker(
                    delegate = delegate,
                    onError = { message -> onErrorState.value(message) },
                )
            }
        }
    }
}

@Composable
actual fun rememberDeckArchiveShareLauncher(
    onError: (String) -> Unit,
): DeckArchiveShareLauncher {
    val onErrorState = rememberUpdatedState(onError)

    return remember {
        object : DeckArchiveShareLauncher {
            override fun shareArchive(
                filePath: String,
                fileName: String,
            ) {
                shareArchiveFile(
                    filePath = filePath,
                    fileName = fileName,
                    onError = { message -> onErrorState.value(message) },
                )
            }
        }
    }
}

private class IOSDeckArchivePickerDelegate(
    private val onArchivePicked: (String) -> Unit,
    private val onError: (String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentAtURL: NSURL,
    ) {
        handlePickedUrl(
            controller = controller,
            url = didPickDocumentAtURL,
        )
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val firstUrl = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        handlePickedUrl(
            controller = controller,
            url = firstUrl,
        )
    }

    private fun handlePickedUrl(
        controller: UIDocumentPickerViewController,
        url: NSURL?,
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        if (url == null) {
            onError("No archive selected")
            return
        }
        val copiedUri = copyPickedArchiveToTemp(url)
        if (copiedUri != null) {
            onArchivePicked(copiedUri)
        } else {
            onError("Failed to import selected archive")
        }
    }
}

private fun openArchivePicker(
    delegate: IOSDeckArchivePickerDelegate,
    onError: (String) -> Unit,
) {
    val top = topViewController()
    val host = when (top) {
        is UIAlertController -> top.presentingViewController
        else -> top
    }
    if (host == null) {
        onError("Unable to present archive picker")
        return
    }

    val picker = UIDocumentPickerViewController(
        documentTypes = listOf(
            "public.zip-archive",
            "com.pkware.zip-archive",
            "public.data",
        ),
        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
    ).apply {
        allowsMultipleSelection = false
        this.delegate = delegate
    }

    if (top is UIAlertController) {
        top.dismissViewControllerAnimated(
            false,
            completion = { host.presentViewController(picker, animated = true, completion = null) },
        )
    } else {
        host.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun shareArchiveFile(
    filePath: String,
    fileName: String,
    onError: (String) -> Unit,
) {
    val normalizedPath = normalizePath(filePath)
    if (normalizedPath.isBlank() || !NSFileManager.defaultManager.fileExistsAtPath(normalizedPath)) {
        onError("Archive file does not exist")
        return
    }

    val top = topViewController()
    val host = when (top) {
        is UIAlertController -> top.presentingViewController
        else -> top
    }
    if (host == null) {
        onError("Unable to present share sheet")
        return
    }

    val url = NSURL.fileURLWithPath(normalizedPath)
    val activityController = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null,
    )
    activityController.popoverPresentationController?.let { popover ->
        popover.sourceView = host.view
        popover.sourceRect = host.view.bounds
    }

    if (top is UIAlertController) {
        top.dismissViewControllerAnimated(
            false,
            completion = {
                host.presentViewController(
                    activityController,
                    animated = true,
                    completion = null,
                )
            },
        )
    } else {
        host.presentViewController(activityController, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyPickedArchiveToTemp(url: NSURL): String? {
    val hasSecurityScope = url.startAccessingSecurityScopedResource()
    try {
        val tempPath = normalizePath("${NSTemporaryDirectory()}/rwdeck_import_${NSUUID().UUIDString}.rwdeck")
        val tempUrl = NSURL.fileURLWithPath(tempPath)
        NSFileManager.defaultManager.removeItemAtURL(tempUrl, error = null)
        val copied = NSFileManager.defaultManager.copyItemAtURL(
            srcURL = url,
            toURL = tempUrl,
            error = null,
        )
        if (!copied) return null
        return NSURL.fileURLWithPath(tempPath).absoluteString
    } finally {
        if (hasSecurityScope) {
            url.stopAccessingSecurityScopedResource()
        }
    }
}

private fun topViewController(): UIViewController? {
    val application = UIApplication.sharedApplication
    val keyWindow = application.keyWindow
        ?: (application.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow)
        ?: return null

    var top = keyWindow.rootViewController ?: return null
    while (top.presentedViewController != null) {
        top = top.presentedViewController!!
    }
    return top
}

private fun normalizePath(path: String): String {
    return if (path.startsWith("file://")) {
        NSURL.URLWithString(path)?.path ?: path.removePrefix("file://")
    } else {
        path
    }
}
