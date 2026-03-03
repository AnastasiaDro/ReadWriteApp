package com.cerebus.readwrite.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object StudentNavigationState {
    private val _studentChangedVersion = MutableStateFlow(0L)
    val studentChangedVersion = _studentChangedVersion.asStateFlow()

    fun notifyStudentChanged() {
        _studentChangedVersion.value = _studentChangedVersion.value + 1L
    }
}
