package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val studentDeckRepository: StudentDeckRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangeStudentUiState())
    val uiState: StateFlow<ChangeStudentUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<ChangeStudentEffect?>(null)
    val effects: StateFlow<ChangeStudentEffect?> = _effects.asStateFlow()
    private val activeStudentId = MutableStateFlow<String?>(preferencesRepository.getLastActiveStudentId())

    init {
        observeStudents()
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

    private fun observeStudents() {
        viewModelScope.launch {
            combine(
                studentDeckRepository.observeStudentsWithDecksOrderedByCreation(),
                activeStudentId,
            ) { studentsWithDecks, preferredId ->
                val resolvedActiveId = resolveActiveStudentId(
                    students = studentsWithDecks,
                    preferredId = preferredId,
                )
                val orderedStudents = if (resolvedActiveId.isNullOrBlank()) {
                    studentsWithDecks
                } else {
                    studentsWithDecks.sortedByDescending { it.student.id == resolvedActiveId }
                }
                ChangeStudentSnapshot(
                    activeStudentId = resolvedActiveId,
                    students = orderedStudents.map { relation ->
                        ChangeStudentListItem(
                            studentId = relation.student.id,
                            studentName = relation.student.name,
                            lastLessonDeck = relation.decks.firstOrNull(),
                        )
                    },
                )
            }.collect { snapshot ->
                if (snapshot.activeStudentId != activeStudentId.value) {
                    activeStudentId.value = snapshot.activeStudentId
                }

                when {
                    snapshot.activeStudentId.isNullOrBlank() -> {
                        if (!preferencesRepository.getLastActiveStudentId().isNullOrBlank()) {
                            preferencesRepository.clearLastActiveStudentId()
                        }
                    }

                    preferencesRepository.getLastActiveStudentId() != snapshot.activeStudentId -> {
                        preferencesRepository.setLastActiveStudentId(snapshot.activeStudentId)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        students = snapshot.students,
                        activeStudentId = snapshot.activeStudentId,
                    )
                }
            }
        }
    }

    private fun selectStudent(studentId: String, deckIdToOpen: String?) {
        viewModelScope.launch {
            preferencesRepository.setLastActiveStudentId(studentId)
            activeStudentId.value = studentId
            _effects.value = if (deckIdToOpen == null) {
                ChangeStudentEffect.NavigateBack
            } else {
                ChangeStudentEffect.OpenDeck(deckIdToOpen)
            }
        }
    }

    private fun resolveActiveStudentId(
        students: List<StudentWithDecks>,
        preferredId: String?,
    ): String? {
        if (students.isEmpty()) return null
        if (!preferredId.isNullOrBlank()) {
            if (students.any { it.student.id == preferredId }) {
                return preferredId
            }
            return students.first().student.id
        }
        return null
    }
}

private data class ChangeStudentSnapshot(
    val activeStudentId: String?,
    val students: List<ChangeStudentListItem>,
)
