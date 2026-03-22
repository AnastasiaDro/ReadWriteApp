package com.cerebus.create_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.utils.nowMillis
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.customkeyboard.isNeighborKeyboardSlip
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.orEmpty

class DeckGalleryViewModel(
    private val deckId: String,
    private val initialCardId: String?,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val preferencesRepository: PreferencesRepository,
    private val studentRepository: StudentRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeckGalleryUiState(deckId = deckId))
    val uiState: StateFlow<DeckGalleryUiState> = _uiState.asStateFlow()

    private var pendingInitialCardId: String? = initialCardId
    private var observeDeckJob: Job? = null
    private var feedbackJob: Job? = null
    private var keyFeedbackJob: Job? = null

    private var studentId: String = ""
    private var studiedSymbols: Set<String> = emptySet()
    private var preventWrongKeyPress: Boolean = true
    private var allowNeighborTypos: Boolean = true
    private var neighborTypoSensitivity: NeighborTypoSensitivity = NeighborTypoSensitivity.Strict
    private var keyboardPressDelayMs: Long = KeyboardPressDelay.Normal.intervalMs
    private var lastHandledKeyPressAtEpochMillis: Long = 0L

    fun load() {
        observeDeckJob?.cancel()
        if (deckId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        observeDeckJob = viewModelScope.launch {
            studentId = preferencesRepository.getLastActiveStudentId().orEmpty()
            studiedSymbols = loadStudiedSymbols(studentId)
            preventWrongKeyPress = preferencesRepository.getPreventWrongKeyPressEnabled(studentId) ?: true
            allowNeighborTypos = preferencesRepository.getAllowNeighborTyposEnabled(studentId) ?: true
            neighborTypoSensitivity = preferencesRepository.getNeighborTypoSensitivity(studentId)
                ?: NeighborTypoSensitivity.Strict
            keyboardPressDelayMs = (preferencesRepository.getKeyboardPressDelay(studentId)
                ?: KeyboardPressDelay.Normal).intervalMs
            _uiState.update {
                it.copy(
                    isLoading = true,
                    deckId = deckId,
                    isShiftEnabled = preferencesRepository.getKeyboardShiftEnabled(studentId) == true,
                    isInputHintEnabled = preferencesRepository.getGalleryInputHintEnabled(studentId) == true,
                    hideDigitsOnTightScreen = preferencesRepository
                        .getHideDigitsOnTightScreenEnabled(studentId) ?: true,
                )
            }

            combine(
                deckRepository.observeDeckById(deckId),
                flashcardRepository.observeFlashcardsByDeckId(deckId),
            ) { deck, cards ->
                deck to cards
            }.collect { (deck, cards) ->
                val currentState = _uiState.value
                val previousCardId = currentState.currentCard?.id
                val preferredIndex = pendingInitialCardId?.let { cardId ->
                    cards.indexOfFirst { it.id == cardId }.takeIf { it >= 0 }
                }
                if (preferredIndex != null) {
                    pendingInitialCardId = null
                }
                val persistedIndex = previousCardId?.let { cardId ->
                    cards.indexOfFirst { it.id == cardId }.takeIf { it >= 0 }
                }
                val nextIndex = (preferredIndex ?: persistedIndex ?: currentState.currentIndex)
                    .coerceIn(0, cards.lastIndex.coerceAtLeast(0))
                val nextCardId = cards.getOrNull(nextIndex)?.id
                val cardChanged = currentState.isLoading || nextCardId != previousCardId

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        deckId = deckId,
                        cards = cards,
                        currentIndex = if (cards.isEmpty()) 0 else nextIndex,
                    )
                }

                if (cardChanged) {
                    resetTrainingStateForCurrentCard()
                } else {
                    refreshActiveSymbols()
                }
            }
        }
    }

    fun showPrevious() {
        openCardAt((_uiState.value.currentIndex - 1).coerceAtLeast(0))
    }

    fun showNext() {
        openCardAt((_uiState.value.currentIndex + 1).coerceAtMost(_uiState.value.cards.lastIndex))
    }

    fun showCard(index: Int) {
        openCardAt(index.coerceIn(0, _uiState.value.cards.lastIndex.coerceAtLeast(0)))
    }

    fun onShiftChanged(isEnabled: Boolean) {
        _uiState.update { it.copy(isShiftEnabled = isEnabled) }
        if (studentId.isNotBlank()) {
            runCatching {
                preferencesRepository.setKeyboardShiftEnabled(studentId, isEnabled)
            }
        }
    }

    fun onSymbolPressed(symbol: String) {
        val current = _uiState.value
        val currentCard = current.currentCard ?: return
        if (current.feedback != null) return
        if (!consumeKeyboardPressThrottle()) return

        if (!preventWrongKeyPress) {
            _uiState.update { it.copy(answerInput = it.answerInput + symbol) }
            return
        }

        val expectedSymbol = expectedSymbolForPressed(
            answerInput = current.answerInput,
            expectedAnswer = currentCard.name,
            pressedSymbol = symbol,
        )
        val acceptsPressedSymbol = acceptsPressedSymbol(
            answerInput = current.answerInput,
            expectedAnswer = currentCard.name,
            pressedSymbol = symbol,
        )

        if (acceptsPressedSymbol && expectedSymbol != null) {
            _uiState.update { it.copy(answerInput = it.answerInput + symbol) }
            triggerTypingFeedback(symbol, TrainingKeyboardFeedbackType.Correct)
            return
        }

        if (expectedSymbol != null) {
            val isStrictSlip = isNeighborKeyboardSlip(
                referenceText = currentCard.name,
                expectedSymbol = expectedSymbol,
                pressedSymbol = symbol,
                sensitivity = NeighborTypoSensitivity.Normal,
            )
            if (isStrictSlip) {
                val shouldTreatAsSlip = allowNeighborTypos && isNeighborKeyboardSlip(
                    referenceText = currentCard.name,
                    expectedSymbol = expectedSymbol,
                    pressedSymbol = symbol,
                    sensitivity = neighborTypoSensitivity,
                )
                triggerTypingFeedback(
                    symbol = symbol,
                    type = if (shouldTreatAsSlip) {
                        TrainingKeyboardFeedbackType.Slip
                    } else {
                        TrainingKeyboardFeedbackType.Wrong
                    },
                )
                return
            }
        }

        triggerTypingFeedback(symbol, TrainingKeyboardFeedbackType.Wrong)
    }

    fun onBackspacePressed() {
        val current = _uiState.value
        if (current.feedback != null) return
        if (!consumeKeyboardPressThrottle()) return
        _uiState.update {
            it.copy(answerInput = it.answerInput.dropLast(1))
        }
    }

    fun onSubmitPressed(strings: DeckGalleryStrings) {
        val current = _uiState.value
        val currentCard = current.currentCard ?: return
        if (current.feedback != null) return

        val userInput = current.answerInput.canonicalizeOptionalSpacesForExpected(currentCard.name)
        val isCorrect = userInput.trim().equals(currentCard.name.trim(), ignoreCase = true)
        if (!isCorrect) {
            showAttemptFeedback(
                feedback = DeckGalleryFeedbackUi(
                    message = strings.wrongFeedback,
                    emoji = "✏️",
                ),
                afterDelay = {
                    _uiState.update { state -> state.copy(feedback = null) }
                },
            )
            return
        }

        showAttemptFeedback(
            feedback = DeckGalleryFeedbackUi(
                message = strings.correctFeedback,
                emoji = "✅",
            ),
            afterDelay = {
                resetAttemptForCurrentCard(keepHintVisible = current.isHintVisible)
            },
        )
    }

    fun onShowWordToggle(isEnabled: Boolean) {
        val current = _uiState.value
        if (current.feedback != null) return
        _uiState.update {
            it.copy(
                usedShowWord = it.usedShowWord || isEnabled,
                isHintVisible = isEnabled,
            )
        }
    }

    fun onHintToggle(isEnabled: Boolean) {
        val current = _uiState.value
        if (current.feedback != null) return
        _uiState.update {
            it.copy(
                usedHint = it.usedHint || isEnabled,
                isInputHintEnabled = isEnabled,
            )
        }
        if (studentId.isNotBlank()) {
            runCatching {
                preferencesRepository.setGalleryInputHintEnabled(studentId, isEnabled)
            }
        }
    }

    fun onSimplifyKeyboardToggle(isEnabled: Boolean) {
        val current = _uiState.value
        if (current.feedback != null) return
        _uiState.update {
            it.copy(
                usedSimplifiedKeyboard = it.usedSimplifiedKeyboard || isEnabled,
                isSimplifiedKeyboardEnabled = isEnabled,
            )
        }
        refreshActiveSymbols()
    }

    private fun openCardAt(index: Int) {
        val current = _uiState.value
        if (current.cards.isEmpty()) return
        val safeIndex = index.coerceIn(0, current.cards.lastIndex)
        if (safeIndex == current.currentIndex) return
        feedbackJob?.cancel()
        keyFeedbackJob?.cancel()
        _uiState.update { it.copy(currentIndex = safeIndex) }
        resetTrainingStateForCurrentCard()
    }

    private fun resetTrainingStateForCurrentCard() {
        lastHandledKeyPressAtEpochMillis = 0L
        feedbackJob?.cancel()
        keyFeedbackJob?.cancel()
        val keepHintVisible = _uiState.value.isHintVisible
        _uiState.update { state ->
            state.copy(
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
                answerInput = "",
                isHintVisible = keepHintVisible,
                isInputHintEnabled = state.isInputHintEnabled,
                isSimplifiedKeyboardEnabled = false,
                usedHint = false,
                usedShowWord = false,
                usedSimplifiedKeyboard = false,
                feedback = null,
            )
        }
        refreshActiveSymbols()
    }

    private fun resetAttemptForCurrentCard(
        keepHintVisible: Boolean,
    ) {
        lastHandledKeyPressAtEpochMillis = 0L
        keyFeedbackJob?.cancel()
        _uiState.update { state ->
            state.copy(
                answerInput = "",
                isHintVisible = keepHintVisible,
                isInputHintEnabled = state.isInputHintEnabled,
                isSimplifiedKeyboardEnabled = false,
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
                usedHint = false,
                usedShowWord = false,
                usedSimplifiedKeyboard = false,
                feedback = null,
            )
        }
        refreshActiveSymbols()
    }


    private fun expectedSymbolForPressed(
        answerInput: String,
        expectedAnswer: String,
        pressedSymbol: String,
    ): String? {
        val alignment = answerInputAlignment(answerInput, expectedAnswer)
        val expectedSymbol = expectedAnswer.getOrNull(alignment.nextExpectedIndex)?.toString() ?: return null
        if (expectedSymbol != " " || pressedSymbol == " ") return expectedSymbol
        return expectedAnswer
            .substring(alignment.nextExpectedIndex)
            .firstOrNull { !it.isWhitespace() }
            ?.toString()
    }


    private fun acceptsPressedSymbol(
        answerInput: String,
        expectedAnswer: String,
        pressedSymbol: String,
    ): Boolean {
        val alignment = answerInputAlignment(answerInput, expectedAnswer)
        val nextIndex = alignment.nextExpectedIndex
        val nextExpectedChar = expectedAnswer.getOrNull(nextIndex) ?: return false
        if (!nextExpectedChar.isWhitespace()) {
            return pressedSymbol.matchesExpectedSymbol(nextExpectedChar.toString())
        }
        if (pressedSymbol == " ") return true
        val nextLetter = expectedAnswer
            .substring(nextIndex)
            .firstOrNull { !it.isWhitespace() }
            ?: return false
        return pressedSymbol.matchesExpectedSymbol(nextLetter.toString())
    }

    private fun refreshActiveSymbols() {
        val currentCard = _uiState.value.currentCard
        val cardSymbols = currentCard?.extractKeyboardSymbols().orEmpty()
        _uiState.update { state ->
            state.copy(
                activeSymbols = if (state.isSimplifiedKeyboardEnabled) {
                    cardSymbols
                } else {
                    studiedSymbols + cardSymbols
                }
            )
        }
    }

    private fun consumeKeyboardPressThrottle(): Boolean {
        val now = nowMillis()
        if (now - lastHandledKeyPressAtEpochMillis < keyboardPressDelayMs) return false
        lastHandledKeyPressAtEpochMillis = now
        return true
    }

    private fun triggerTypingFeedback(
        symbol: String,
        type: TrainingKeyboardFeedbackType,
    ) {
        keyFeedbackJob?.cancel()
        _uiState.update {
            it.copy(
                keyboardFeedbackKey = symbol.lowercase(),
                keyboardFeedbackType = type,
                inputFeedbackType = type,
            )
        }

        keyFeedbackJob = viewModelScope.launch {
            delay(180L)
            _uiState.update {
                it.copy(
                    keyboardFeedbackKey = null,
                    keyboardFeedbackType = null,
                    inputFeedbackType = null,
                )
            }
        }
    }

    private fun showAttemptFeedback(
        feedback: DeckGalleryFeedbackUi,
        afterDelay: () -> Unit,
    ) {
        feedbackJob?.cancel()
        _uiState.update { it.copy(feedback = feedback) }
        feedbackJob = viewModelScope.launch {
            delay(GALLERY_FEEDBACK_DURATION_MS)
            afterDelay()
        }
    }

    private suspend fun loadStudiedSymbols(studentId: String): Set<String> {
        if (studentId.isBlank()) return emptySet()
        return studentRepository.getActiveLettersById(studentId)
            .orEmpty()
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .map { it.toString() }
            .toSet()
    }

    override fun onCleared() {
        observeDeckJob?.cancel()
        feedbackJob?.cancel()
        keyFeedbackJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val GALLERY_FEEDBACK_DURATION_MS = 1000L
    }
}



private fun Flashcard.extractKeyboardSymbols(): Set<String> {
    return name
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .map { it.toString() }
        .toSet()
}

private fun String.canonicalizeOptionalSpacesForExpected(expectedAnswer: String): String {
    val normalizedUser = filterNot(Char::isWhitespace)
    val normalizedExpected = expectedAnswer.filterNot(Char::isWhitespace)
    return if (normalizedUser.equals(normalizedExpected, ignoreCase = true)) {
        expectedAnswer
    } else {
        this
    }
}
