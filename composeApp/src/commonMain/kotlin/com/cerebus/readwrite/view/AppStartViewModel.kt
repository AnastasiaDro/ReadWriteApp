package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.student.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StartRouteTarget {
    HAS_STUDENTS,
    NO_STUDENTS,
}

data class AppStartUiState(
    val isLoading: Boolean = true,
    val pendingRoute: StartRouteTarget? = null,
)

class AppStartViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppStartUiState())
    val uiState: StateFlow<AppStartUiState> = _uiState.asStateFlow()

    init {
        resolveStartRoute()
    }

    fun onRouteHandled() {
        _uiState.update { it.copy(pendingRoute = null) }
    }

    private fun resolveStartRoute() {
        viewModelScope.launch {
            val hasStudents = studentRepository.hasAnyStudents()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    pendingRoute = if (hasStudents) {
                        StartRouteTarget.HAS_STUDENTS
                    } else {
                        StartRouteTarget.NO_STUDENTS
                    },
                )
            }
        }
    }
}
