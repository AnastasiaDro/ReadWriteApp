package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveStudentUiState(
    val isLoading: Boolean = true,
    val studentId: String? = null,
    val studentName: String = "",
    val decks: List<Deck> = emptyList(),
    val lastLessonDeck: Deck? = null,
)

sealed interface ActiveStudentAction {
    data object OnStartClick : ActiveStudentAction
    data object OnChangeStudentClick : ActiveStudentAction
    data object OnMoreDecksClick : ActiveStudentAction
    data object OnCreateDeckClick : ActiveStudentAction
    data class OnDeckClick(val deckId: String) : ActiveStudentAction
}

sealed interface ActiveStudentEffect {
    data class OpenDeck(val deckId: String) : ActiveStudentEffect
    data class OpenGame(val deckId: String) : ActiveStudentEffect
    data class OpenDeckList(val openCreateDialog: Boolean) : ActiveStudentEffect
    data object OpenChangeStudent : ActiveStudentEffect
}

class ActiveStudentViewModel(
    private val studentRepository: StudentRepository,
    private val studentDeckRepository: StudentDeckRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveStudentUiState())
    val uiState: StateFlow<ActiveStudentUiState> = _uiState.asStateFlow()
    private val _effects = MutableStateFlow<ActiveStudentEffect?>(null)
    val effects: StateFlow<ActiveStudentEffect?> = _effects.asStateFlow()

    init {
        loadActiveStudent()
    }

    fun onAction(action: ActiveStudentAction) {
        when (action) {
            ActiveStudentAction.OnStartClick -> {
                val deckId = _uiState.value.lastLessonDeck?.id ?: return
                _effects.value = ActiveStudentEffect.OpenGame(deckId)
            }

            ActiveStudentAction.OnChangeStudentClick -> {
                _effects.value = ActiveStudentEffect.OpenChangeStudent
            }

            ActiveStudentAction.OnMoreDecksClick -> {
                _effects.value = ActiveStudentEffect.OpenDeckList(openCreateDialog = false)
            }

            ActiveStudentAction.OnCreateDeckClick -> {
                _effects.value = ActiveStudentEffect.OpenDeckList(openCreateDialog = true)
            }

            is ActiveStudentAction.OnDeckClick -> {
                _effects.value = ActiveStudentEffect.OpenDeck(action.deckId)
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    fun refresh() {
        loadActiveStudent()
    }

    private fun loadActiveStudent() {
        viewModelScope.launch {
            val resolvedStudentId = resolveStudentId() ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            preferencesRepository.setLastActiveStudentId(resolvedStudentId)

            val relation = studentDeckRepository.getStudentWithDecks(resolvedStudentId)
            if (relation != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        studentId = relation.student.id,
                        studentName = relation.student.name,
                        decks = relation.decks,
                        lastLessonDeck = relation.decks.firstOrNull(),
                    )
                }
                return@launch
            }

            val student = studentRepository.getStudentById(resolvedStudentId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    studentId = student?.id,
                    studentName = student?.name.orEmpty(),
                    decks = emptyList(),
                    lastLessonDeck = null,
                )
            }
        }
    }

    private suspend fun resolveStudentId(): String? {
        val storedId = preferencesRepository.getLastActiveStudentId()
        if (!storedId.isNullOrBlank() && studentRepository.getStudentById(storedId) != null) {
            return storedId
        }
        return studentRepository.getFirstStudentId()
    }
}
