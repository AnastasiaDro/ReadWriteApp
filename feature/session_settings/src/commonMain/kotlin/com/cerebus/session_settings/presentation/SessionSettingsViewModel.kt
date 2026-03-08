package com.cerebus.session_settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.utils.CustomResult
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionSettingsViewModel(
    private val studentId: String,
    private val prefsRepository: StudentPrefsRepository,
    private val deckRepository: DeckRepository,
    private val studentDeckRepository: StudentDeckRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SessionSettingsState())
    val state: StateFlow<SessionSettingsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SessionSettingsEffect>()
    val effects: SharedFlow<SessionSettingsEffect> = _effects.asSharedFlow()

    private var loadedPrefs: StudentSrsPrefs? = null
    private var reviewsChangedByUser: Boolean = false
    private var isLoaded: Boolean = false
    private var initialActiveDeckIds: Set<String> = emptySet()

    fun onIntent(intent: SessionSettingsIntent) {
        when (intent) {
            SessionSettingsIntent.Load -> load()
            is SessionSettingsIntent.ChangeNewCards -> onNewCardsChanged(intent.value)
            is SessionSettingsIntent.ChangeReviews -> onReviewsChanged(intent.value)
            is SessionSettingsIntent.ChangeLearnMoreStep -> {
                _state.update { it.copy(learnMoreStep = intent.value.coerceAtLeast(0)) }
            }
            is SessionSettingsIntent.ChangeGuidedHintSuccessThreshold -> {
                _state.update { it.copy(guidedHintSuccessThreshold = intent.value.coerceIn(0, 5)) }
            }
            is SessionSettingsIntent.ChangeMaxNewPerDay -> {
                _state.update { it.copy(maxNewCardsPerDay = intent.value.coerceAtLeast(0)) }
            }
            is SessionSettingsIntent.ChangeAllowNearMatch -> {
                _state.update { it.copy(allowNearMatch = intent.value) }
            }
            is SessionSettingsIntent.ToggleDeck -> toggleDeck(intent.deckId, intent.isActive)
            SessionSettingsIntent.SaveClicked -> save()
            SessionSettingsIntent.CancelClicked -> emitClose()
        }
    }

    private fun load() {
        if (isLoaded) return
        isLoaded = true
        viewModelScope.launch {
            runCatching {
                val prefs = prefsRepository.getPrefs(studentId)
                val allDecks = deckRepository.getAllDecks()
                val studentDecks = studentDeckRepository.getStudentWithDecks(studentId)
                    ?.decks
                    .orEmpty()
                    .map { it.id }
                    .toSet()
                Triple(prefs, allDecks, studentDecks)
            }.onSuccess { (prefs, allDecks, selectedDeckIds) ->
                loadedPrefs = prefs
                reviewsChangedByUser = false
                initialActiveDeckIds = selectedDeckIds
                _state.value = SessionSettingsState(
                    newCardsPerSession = prefs.newCardsPerSession,
                    reviewsPerSession = prefs.reviewsPerSession,
                    learnMoreStep = prefs.learnMoreStep,
                    guidedHintSuccessThreshold = prefs.guidedHintSuccessThreshold,
                    maxNewCardsPerDay = prefs.maxNewCardsPerDay,
                    allowNearMatch = prefs.allowNearMatch,
                    deckOptions = allDecks.map { deck ->
                        SessionDeckOptionUi(
                            deckId = deck.id,
                            deckName = deck.name,
                            isActive = deck.id in selectedDeckIds,
                        )
                    },
                    isLoading = false,
                    isSaving = false,
                )
            }.onFailure {
                _state.update { it.copy(isLoading = false, isSaving = false) }
                _effects.emit(SessionSettingsEffect.ShowLoadError)
            }
        }
    }

    private fun onNewCardsChanged(value: Int) {
        val safeValue = value.coerceAtLeast(0)
        _state.update { current ->
            current.copy(
                newCardsPerSession = safeValue,
                reviewsPerSession = if (reviewsChangedByUser) {
                    current.reviewsPerSession
                } else {
                    safeValue * 3
                },
            )
        }
    }

    private fun onReviewsChanged(value: Int) {
        reviewsChangedByUser = true
        _state.update { it.copy(reviewsPerSession = value.coerceAtLeast(0)) }
    }

    private fun toggleDeck(deckId: String, isActive: Boolean) {
        _state.update { current ->
            current.copy(
                deckOptions = current.deckOptions.map { option ->
                    if (option.deckId == deckId) {
                        option.copy(isActive = isActive)
                    } else {
                        option
                    }
                }
            )
        }
    }

    private fun save() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val base = loadedPrefs ?: StudentSrsPrefs(studentId = studentId)
            val snapshot = _state.value
            val updatedPrefs = base.copy(
                newCardsPerSession = snapshot.newCardsPerSession,
                reviewsPerSession = snapshot.reviewsPerSession,
                learnMoreStep = snapshot.learnMoreStep,
                guidedHintSuccessThreshold = snapshot.guidedHintSuccessThreshold,
                maxNewCardsPerDay = snapshot.maxNewCardsPerDay,
                allowNearMatch = snapshot.allowNearMatch,
            )
            val selectedDeckIds = snapshot.deckOptions
                .asSequence()
                .filter { it.isActive }
                .map { it.deckId }
                .toSet()
            val deckIdsToAssign = selectedDeckIds - initialActiveDeckIds
            val deckIdsToUnassign = initialActiveDeckIds - selectedDeckIds

            runCatching {
                prefsRepository.savePrefs(updatedPrefs)
                if (deckIdsToAssign.isNotEmpty()) {
                    when (
                        val result = studentDeckRepository.assignDecksToStudent(
                        studentId = studentId,
                        deckIds = deckIdsToAssign.toList(),
                    )
                    ) {
                        is CustomResult.Failure -> throw result.error
                        is CustomResult.Success -> Unit
                    }
                }
                if (deckIdsToUnassign.isNotEmpty()) {
                    when (
                        val result = studentDeckRepository.unassignDecksFromStudent(
                        studentId = studentId,
                        deckIds = deckIdsToUnassign.toList(),
                    )
                    ) {
                        is CustomResult.Failure -> throw result.error
                        is CustomResult.Success -> Unit
                    }
                }
            }.onSuccess {
                loadedPrefs = updatedPrefs
                initialActiveDeckIds = selectedDeckIds
                _state.update { it.copy(isSaving = false) }
                _effects.emit(SessionSettingsEffect.CloseScreen)
            }.onFailure {
                _state.update { it.copy(isSaving = false) }
                _effects.emit(SessionSettingsEffect.ShowSaveError)
            }
        }
    }

    private fun emitClose() {
        viewModelScope.launch {
            _effects.emit(SessionSettingsEffect.CloseScreen)
        }
    }
}
