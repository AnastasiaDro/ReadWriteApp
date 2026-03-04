package com.cerebus.readwrite.media

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberCoverImagePicker(
    onImagePicked: (String) -> Unit,
    onError: (String) -> Unit,
): CoverImagePicker {
    val context = LocalContext.current
    val onImagePickedState = rememberUpdatedState(onImagePicked)
    val onErrorState = rememberUpdatedState(onError)

    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            onImagePickedState.value(uri.toString())
        }
    }

    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            cameraOutputUri?.let { onImagePickedState.value(it.toString()) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraOutputUri = uri
            cameraCapture.launch(uri)
        } else {
            onErrorState.value("Camera permission denied")
        }
    }

    return remember {
        object : CoverImagePicker {
            override fun openGallery() {
                galleryPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            override fun openCamera() {
                val permissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED

                if (permissionGranted) {
                    val uri = createTempImageUri(context)
                    cameraOutputUri = uri
                    cameraCapture.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "deck_covers").apply { mkdirs() }
    val image = File.createTempFile("deck_cover_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        image,
    )
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
