package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
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

    val delegate = remember {
        IOSImagePickerDelegate(
            onImagePicked = { uri -> onImagePickedState.value(uri) },
            onError = { message -> onErrorState.value(message) },
        )
    }

    return remember(delegate) {
        object : CoverImagePicker {
            override fun openGallery() {
                openPicker(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                    delegate = delegate,
                    onError = { message -> onErrorState.value(message) },
                )
            }

            override fun openCamera() {
                openPicker(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                    delegate = delegate,
                    onError = { message -> onErrorState.value(message) },
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IOSImagePickerDelegate(
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
        val imageUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerImageURL] as? NSURL
        val resolvedUri = when {
            image != null -> saveImageToLocalFile(image)
            imageUrl != null -> imageUrl.absoluteString
            else -> null
        }

        if (resolvedUri != null) {
            onImagePicked(resolvedUri)
        } else {
            onError("Failed to pick image")
        }

        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun openPicker(
    sourceType: UIImagePickerControllerSourceType,
    delegate: IOSImagePickerDelegate,
    onError: (String) -> Unit,
) {
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        onError("Source is unavailable on this device")
        return
    }

    val host = topViewController()
    if (host == null) {
        onError("Unable to present image picker")
        return
    }

    val picker = UIImagePickerController().apply {
        this.sourceType = sourceType
        this.delegate = delegate
        this.mediaTypes = listOf("public.image")

        if (sourceType == UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) {
            // Restrict camera flow to still images to avoid AVFoundation fallback errors.
            this.cameraCaptureMode = UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto
        }
    }
    host.presentViewController(picker, animated = true, completion = null)
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
