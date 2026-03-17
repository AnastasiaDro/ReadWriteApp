package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable

interface DeckArchivePicker {
    fun openArchivePicker()
}

data class DeckArchiveShareItem(
    val filePath: String,
    val fileName: String,
)

@Composable
expect fun rememberDeckArchivePicker(
    onArchivePicked: (String) -> Unit,
    onError: (String) -> Unit,
): DeckArchivePicker

interface DeckArchiveShareLauncher {
    fun shareArchive(
        filePath: String,
        fileName: String,
    ) {
        shareArchives(
            listOf(
                DeckArchiveShareItem(
                    filePath = filePath,
                    fileName = fileName,
                )
            )
        )
    }

    fun shareArchives(files: List<DeckArchiveShareItem>)
}

@Composable
expect fun rememberDeckArchiveShareLauncher(
    onError: (String) -> Unit,
): DeckArchiveShareLauncher
