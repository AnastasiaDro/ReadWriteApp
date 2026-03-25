package com.cerebus.readwrite.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StudentImportNavigationState {
    private val _pendingImportStudentArchiveUri = MutableStateFlow<String?>(null)
    val pendingImportStudentArchiveUri: StateFlow<String?> = _pendingImportStudentArchiveUri.asStateFlow()

    fun requestImportStudentArchive(uri: String) {
        if (uri.isBlank()) return
        _pendingImportStudentArchiveUri.value = uri
    }

    fun consumePendingImportStudentArchive(uri: String) {
        if (_pendingImportStudentArchiveUri.value == uri) {
            _pendingImportStudentArchiveUri.value = null
        }
    }
}
