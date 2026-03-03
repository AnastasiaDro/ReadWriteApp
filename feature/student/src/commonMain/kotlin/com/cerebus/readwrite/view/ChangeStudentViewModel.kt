package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import com.cerebus.readwrite.navigation.StudentNavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangeStudentListItem(
    val studentId: String,
    val studentName: String,
    val lastLessonDeck: Deck?,
)

data class ChangeStudentUiState(
    val isLoading: Boolean = true,
    val students: List<ChangeStudentListItem> = emptyList(),
    val activeStudentId: String? = null,
)

sealed interface ChangeStudentAction {
    data class OnStudentClick(val studentId: String) : ChangeStudentAction
    data class OnDeckClick(val studentId: String, val deckId: String) : ChangeStudentAction
    data object OnCreateStudentClick : ChangeStudentAction
}

sealed interface ChangeStudentEffect {
    data object NavigateBack : ChangeStudentEffect
    data class OpenDeck(val deckId: String) : ChangeStudentEffect
    data object OpenCreateStudent : ChangeStudentEffect
}

class ChangeStudentViewModel(
    private val studentRepository: StudentRepository,
    private val studentDeckRepository: StudentDeckRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangeStudentUiState())
    val uiState: StateFlow<ChangeStudentUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<ChangeStudentEffect?>(null)
    val effects: StateFlow<ChangeStudentEffect?> = _effects.asStateFlow()

    init {
        loadStudents()
    }

    fun onAction(action: ChangeStudentAction) {
        when (action) {
            is ChangeStudentAction.OnStudentClick -> {
                selectStudent(studentId = action.studentId, deckIdToOpen = null)
            }

            is ChangeStudentAction.OnDeckClick -> {
                selectStudent(studentId = action.studentId, deckIdToOpen = action.deckId)
            }

            ChangeStudentAction.OnCreateStudentClick -> {
                _effects.value = ChangeStudentEffect.OpenCreateStudent
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun loadStudents() {
        viewModelScope.launch {
            val activeStudentId = preferencesRepository.getLastActiveStudentId()
            val students = studentRepository.getAllStudentsOrderedByCreation()
            val orderedStudents = if (activeStudentId.isNullOrBlank()) {
                students
            } else {
                students.sortedByDescending { it.id == activeStudentId }
            }

            val items = orderedStudents.map { student ->
                val lastDeck = studentDeckRepository
                    .getStudentWithDecks(student.id)
                    ?.decks
                    ?.firstOrNull()

                ChangeStudentListItem(
                    studentId = student.id,
                    studentName = student.name,
                    lastLessonDeck = lastDeck,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    students = items,
                    activeStudentId = activeStudentId,
                )
            }
        }
    }

    private fun selectStudent(studentId: String, deckIdToOpen: String?) {
        viewModelScope.launch {
            preferencesRepository.setLastActiveStudentId(studentId)
            StudentNavigationState.notifyStudentChanged()
            _effects.value = if (deckIdToOpen == null) {
                ChangeStudentEffect.NavigateBack
            } else {
                ChangeStudentEffect.OpenDeck(deckIdToOpen)
            }
        }
    }
}
