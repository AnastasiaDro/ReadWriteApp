package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable

interface DeckArchivePicker {
    fun openArchivePicker()
}

@Composable
expect fun rememberDeckArchivePicker(
    onArchivePicked: (String) -> Unit,
    onError: (String) -> Unit,
): DeckArchivePicker

interface DeckArchiveShareLauncher {
    fun shareArchive(
        filePath: String,
        fileName: String,
    )
}

@Composable
expect fun rememberDeckArchiveShareLauncher(
    onError: (String) -> Unit,
): DeckArchiveShareLauncher
