package com.cerebus.readwrite.media

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberDeckArchivePicker(
    onArchivePicked: (String) -> Unit,
    onError: (String) -> Unit,
): DeckArchivePicker {
    val context = LocalContext.current
    val onArchivePickedState = rememberUpdatedState(onArchivePicked)
    val onErrorState = rememberUpdatedState(onError)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        onArchivePickedState.value(uri.toString())
    }

    return remember {
        object : DeckArchivePicker {
            override fun openArchivePicker() {
                runCatching {
                    launcher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }.onFailure {
                    onErrorState.value("Unable to open archive picker")
                }
            }
        }
    }
}

@Composable
actual fun rememberDeckArchiveShareLauncher(
    onError: (String) -> Unit,
): DeckArchiveShareLauncher {
    val context = LocalContext.current
    val onErrorState = rememberUpdatedState(onError)

    return remember {
        object : DeckArchiveShareLauncher {
            override fun shareArchive(
                filePath: String,
                fileName: String,
            ) {
                runCatching {
                    val file = File(filePath)
                    require(file.exists()) { "Archive file does not exist" }
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TITLE, fileName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure {
                    onErrorState.value("Unable to share archive")
                }
            }
        }
    }
}
