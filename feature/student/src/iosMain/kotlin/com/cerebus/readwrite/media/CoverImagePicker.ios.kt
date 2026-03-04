package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIAlertController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCoverImagePicker(
    onImagePicked: (String) -> Unit,
    onError: (String) -> Unit,
): CoverImagePicker {
    val onImagePickedState = rememberUpdatedState(onImagePicked)
    val onErrorState = rememberUpdatedState(onError)

    val galleryDelegate = remember {
        IOSGalleryPickerDelegate(
            onImagePicked = { uri -> onImagePickedState.value(uri) },
            onError = { message -> onErrorState.value(message) },
        )
    }

    val cameraDelegate = remember {
        IOSCameraPickerDelegate(
            onImagePicked = { uri -> onImagePickedState.value(uri) },
            onError = { message -> onErrorState.value(message) },
        )
    }

    return remember(galleryDelegate, cameraDelegate) {
        object : CoverImagePicker {
            override fun openGallery() {
                openGalleryPicker(
                    delegate = galleryDelegate,
                    onError = { message -> onErrorState.value(message) },
                )
            }

            override fun openCamera() {
                openCameraPicker(
                    delegate = cameraDelegate,
                    onError = { message -> onErrorState.value(message) },
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IOSGalleryPickerDelegate(
    private val onImagePicked: (String) -> Unit,
    private val onError: (String) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val firstResult = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        val itemProvider = firstResult.itemProvider

        if (!itemProvider.hasItemConformingToTypeIdentifier("public.image")) {
            onError("Selected file is not an image")
            return
        }

        itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { fileUrl, _ ->
            val localPath = fileUrl?.path
            val image = localPath?.let { UIImage.imageWithContentsOfFile(it) }
            val resolvedUri = image?.let(::saveImageToLocalFile)

            // Clean temporary provider copy after importing into app sandbox.
            if (fileUrl != null) {
                NSFileManager.defaultManager.removeItemAtURL(fileUrl, error = null)
            }

            runOnMain {
                if (resolvedUri != null) {
                    onImagePicked(resolvedUri)
                } else {
                    onError("Failed to import image")
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IOSCameraPickerDelegate(
    private val onImagePicked: (String) -> Unit,
    private val onError: (String) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage)
            ?: (didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)
        val resolvedUri = image?.let(::saveImageToLocalFile)

        if (resolvedUri != null) {
            onImagePicked(resolvedUri)
        } else {
            onError("Failed to pick image")
        }

        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun openGalleryPicker(
    delegate: IOSGalleryPickerDelegate,
    onError: (String) -> Unit,
) {
    val top = topViewController()
    val host = when (top) {
        is UIAlertController -> top.presentingViewController
        else -> top
    }

    if (host == null) {
        onError("Unable to present image picker")
        return
    }

    val configuration = PHPickerConfiguration().apply {
        selectionLimit = 1
    }

    val picker = PHPickerViewController(configuration = configuration).apply {
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
private fun openCameraPicker(
    delegate: IOSCameraPickerDelegate,
    onError: (String) -> Unit,
) {
    val sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        onError("Source is unavailable on this device")
        return
    }

    val top = topViewController()
    val host = when (top) {
        is UIAlertController -> top.presentingViewController
        else -> top
    }

    if (host == null) {
        onError("Unable to present image picker")
        return
    }

    val picker = UIImagePickerController().apply {
        this.sourceType = sourceType
        this.delegate = delegate
        this.mediaTypes = listOf("public.image")
        this.cameraCaptureMode =
            UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto
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

@OptIn(ExperimentalForeignApi::class)
private fun saveImageToLocalFile(image: UIImage): String? {
    val data = UIImageJPEGRepresentation(image, 0.92) ?: return null
    val documentsDir = (NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String) ?: NSTemporaryDirectory()
    val filePath = "$documentsDir/deck_cover_${NSUUID().UUIDString}.jpg"
    val file = fopen(filePath, "wb") ?: return null

    try {
        fwrite(data.bytes, 1.convert(), data.length.convert(), file)
    } finally {
        fclose(file)
    }

    return NSURL.fileURLWithPath(filePath).absoluteString
}

private fun runOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue(), block)
}
