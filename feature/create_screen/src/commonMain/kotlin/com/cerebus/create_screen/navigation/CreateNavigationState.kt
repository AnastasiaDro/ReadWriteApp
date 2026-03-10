package com.cerebus.create_screen.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object CreateNavigationState {
    private val _openCreateDialogRequests = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val openCreateDialogRequests: SharedFlow<Unit> = _openCreateDialogRequests.asSharedFlow()
    private val _pendingImportDeckArchiveUri = MutableStateFlow<String?>(null)
    val pendingImportDeckArchiveUri: StateFlow<String?> = _pendingImportDeckArchiveUri.asStateFlow()

    fun requestOpenCreateDialog() {
        _openCreateDialogRequests.tryEmit(Unit)
    }

    fun requestImportDeckArchive(uri: String) {
        if (uri.isBlank()) return
        _pendingImportDeckArchiveUri.value = uri
    }

    fun consumePendingImportDeckArchive(uri: String) {
        if (_pendingImportDeckArchiveUri.value == uri) {
            _pendingImportDeckArchiveUri.value = null
        }
    }
}
