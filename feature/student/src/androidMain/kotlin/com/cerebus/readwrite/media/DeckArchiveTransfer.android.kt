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
            override fun shareArchives(files: List<DeckArchiveShareItem>) {
                runCatching {
                    require(files.isNotEmpty()) { "No archives to share" }
                    val uris = files.map { item ->
                        val file = File(item.filePath)
                        require(file.exists()) { "Archive file does not exist" }
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                    }
                    val mimeType = files
                        .map { item -> item.fileName.lowercase() }
                        .distinct()
                        .singleOrNull()
                        ?.let(::resolveArchiveMimeType)
                        ?: "application/zip"
                    val chooserTitle = files.firstOrNull()?.fileName
                    val shareIntent = if (uris.size == 1) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = mimeType
                            putExtra(Intent.EXTRA_STREAM, uris.first())
                            putExtra(Intent.EXTRA_TITLE, chooserTitle)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = mimeType
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
                            putExtra(Intent.EXTRA_TITLE, chooserTitle)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
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

private fun resolveArchiveMimeType(fileName: String): String = when {
    fileName.endsWith(".rwstudent") -> "application/x-rwstudent"
    fileName.endsWith(".rwdeck") -> "application/x-rwdeck"
    else -> "application/zip"
}
