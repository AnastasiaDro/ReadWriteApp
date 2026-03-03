package com.cerebus.create_screen.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CreateNavigationState {
    private val _openCreateDialogRequests = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val openCreateDialogRequests: SharedFlow<Unit> = _openCreateDialogRequests.asSharedFlow()

    fun requestOpenCreateDialog() {
        _openCreateDialogRequests.tryEmit(Unit)
    }
}
