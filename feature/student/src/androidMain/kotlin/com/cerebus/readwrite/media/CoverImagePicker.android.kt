package com.cerebus.readwrite.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

enum class PendingMediaAction {
    GALLERY,
    CAMERA,
}

@Composable
actual fun rememberCoverImagePicker(
    onImagePicked: (String) -> Unit,
    onError: (String) -> Unit,
): CoverImagePicker {
    val context = LocalContext.current

    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAction by remember { mutableStateOf<PendingMediaAction?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onImagePicked(uri.toString())
    }

    val legacyGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) onImagePicked(uri.toString())
    }

    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            cameraOutputUri?.let { onImagePicked(it.toString()) }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        when (pendingAction) {
            PendingMediaAction.GALLERY -> {
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    true
                } else {
                    results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }

                if (granted) {
                    legacyGalleryPicker.launch("image/*")
                } else {
                    onError("Gallery permission denied")
                }
            }

            PendingMediaAction.CAMERA -> {
                val granted = results[Manifest.permission.CAMERA] == true
                if (granted) {
                    val uri = createTempImageUri(context)
                    cameraOutputUri = uri
                    cameraCapture.launch(uri)
                } else {
                    onError("Camera permission denied")
                }
            }

            null -> Unit
        }
        pendingAction = null
    }

    return remember {
        object : CoverImagePicker {
            override fun openGallery() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    return
                }

                val permissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED

                if (permissionGranted) {
                    legacyGalleryPicker.launch("image/*")
                } else {
                    pendingAction = PendingMediaAction.GALLERY
                    permissionsLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
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
                    pendingAction = PendingMediaAction.CAMERA
                    permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
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
