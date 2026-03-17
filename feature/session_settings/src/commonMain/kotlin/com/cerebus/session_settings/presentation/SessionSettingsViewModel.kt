package com.cerebus.session_settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.utils.CustomResult
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
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
    private val preferencesRepository: PreferencesRepository,
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
            is SessionSettingsIntent.ChangePreventWrongKeyPress -> {
                _state.update { it.copy(preventWrongKeyPress = intent.value) }
            }
            is SessionSettingsIntent.ChangeKeyboardPressDelay -> {
                _state.update { it.copy(keyboardPressDelay = intent.value) }
            }
            is SessionSettingsIntent.ChangeAllowNeighborTypos -> {
                _state.update { it.copy(allowNeighborTypos = intent.value) }
            }
            is SessionSettingsIntent.ChangeNeighborTypoSensitivity -> {
                _state.update { it.copy(neighborTypoSensitivity = intent.value) }
            }
            is SessionSettingsIntent.ChangeFreeNeighborSlipPresses -> {
                _state.update { it.copy(freeNeighborSlipPresses = intent.value.coerceIn(0, 3)) }
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
                val preventWrongKeyPress = preferencesRepository
                    .getPreventWrongKeyPressEnabled(studentId)
                    ?: true
                val keyboardPressDelay = preferencesRepository
                    .getKeyboardPressDelay(studentId)
                    ?: KeyboardPressDelay.Normal
                val allowNeighborTypos = preferencesRepository
                    .getAllowNeighborTyposEnabled(studentId)
                    ?: true
                val neighborTypoSensitivity = preferencesRepository
                    .getNeighborTypoSensitivity(studentId)
                    ?.takeIf { it != NeighborTypoSensitivity.Strict }
                    ?: NeighborTypoSensitivity.Normal
                val freeNeighborSlipPresses = preferencesRepository
                    .getFreeNeighborSlipPresses(studentId)
                    ?: 1
                LoadedSettingsData(
                    prefs = prefs,
                    allDecks = allDecks,
                    selectedDeckIds = studentDecks,
                    preventWrongKeyPress = preventWrongKeyPress,
                    keyboardPressDelay = keyboardPressDelay,
                    allowNeighborTypos = allowNeighborTypos,
                    neighborTypoSensitivity = neighborTypoSensitivity,
                    freeNeighborSlipPresses = freeNeighborSlipPresses,
                )
            }.onSuccess { loaded ->
                val prefs = loaded.prefs
                val allDecks = loaded.allDecks
                val selectedDeckIds = loaded.selectedDeckIds
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
                    preventWrongKeyPress = loaded.preventWrongKeyPress,
                    keyboardPressDelay = loaded.keyboardPressDelay,
                    allowNeighborTypos = loaded.allowNeighborTypos,
                    neighborTypoSensitivity = loaded.neighborTypoSensitivity,
                    freeNeighborSlipPresses = loaded.freeNeighborSlipPresses,
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
                preferencesRepository.setPreventWrongKeyPressEnabled(
                    studentId = studentId,
                    isEnabled = snapshot.preventWrongKeyPress,
                )
                preferencesRepository.setKeyboardPressDelay(
                    studentId = studentId,
                    delay = snapshot.keyboardPressDelay,
                )
                preferencesRepository.setAllowNeighborTyposEnabled(
                    studentId = studentId,
                    isEnabled = snapshot.allowNeighborTypos,
                )
                preferencesRepository.setNeighborTypoSensitivity(
                    studentId = studentId,
                    sensitivity = snapshot.neighborTypoSensitivity,
                )
                preferencesRepository.setFreeNeighborSlipPresses(
                    studentId = studentId,
                    count = snapshot.freeNeighborSlipPresses,
                )
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

private data class LoadedSettingsData(
    val prefs: StudentSrsPrefs,
    val allDecks: List<com.cerebus.data.decks.domain.models.Deck>,
    val selectedDeckIds: Set<String>,
    val preventWrongKeyPress: Boolean,
    val keyboardPressDelay: KeyboardPressDelay,
    val allowNeighborTypos: Boolean,
    val neighborTypoSensitivity: NeighborTypoSensitivity,
    val freeNeighborSlipPresses: Int,
)
