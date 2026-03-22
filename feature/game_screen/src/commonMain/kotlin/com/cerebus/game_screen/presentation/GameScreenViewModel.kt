package com.cerebus.game_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.customkeyboard.isNeighborKeyboardSlip
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.core.game_engine.domain.logic.interleaveReviewAndNewCards
import com.cerebus.core.game_engine.domain.logic.selectBalancedNewCardsByDeck
import com.cerebus.core.game_engine.domain.logic.takeRoundRobinByDeck
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerAndRescheduleUseCase
import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerCommand
import com.cerebus.core.utils.localStartOfDayMillis
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.core.utils.GameLaunchMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val FEEDBACK_DURATION_MS = 1200L
private const val REVIEW_TO_NEW_RATIO = 3
private const val DEFAULT_LEARN_MORE_STEP = 5
private const val MIN_GUIDED_HINT_THRESHOLD = 0
private const val MAX_GUIDED_HINT_THRESHOLD = 5
private const val RANDOM_REVIEW_LIMIT = 10
private const val KEY_FEEDBACK_DURATION_MS = 180L
private const val FAST_TYPING_INTERVAL_MS = 450L
private const val FAST_NEIGHBOR_TYPO_SUGGESTION_THRESHOLD = 3

class GameScreenViewModel(
    deckIds: List<String>,
    private val launchMode: GameLaunchMode,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository,
    private val preferencesRepository: PreferencesRepository,
    private val studentRepository: StudentRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val submitAnswerAndRescheduleUseCase: SubmitAnswerAndRescheduleUseCase,
) : ViewModel() {
    private val selectedDeckIds = deckIds.filter { it.isNotBlank() }.distinct()

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<GameScreenEffect?>(null)
    val effects: StateFlow<GameScreenEffect?> = _effects.asStateFlow()

    private var session: GameSessionData? = null
    private var feedbackJob: Job? = null
    private var keyFeedbackJob: Job? = null
    private var dailyLimitIncrease: Int = 0

    init {
        loadDeckCards()
    }

    fun onAction(action: GameScreenAction) {
        when (action) {
            is GameScreenAction.OnAnswerChanged -> updateAnswer(action.value)
            is GameScreenAction.OnKeyboardSymbolPressed -> handleKeyboardSymbolPress(action.symbol)
            GameScreenAction.OnBackspacePressed -> handleBackspacePressed()
            is GameScreenAction.OnShiftChanged -> updateKeyboardShift(action.isEnabled)
            is GameScreenAction.OnInputHintHelpToggled -> toggleInputHintHelp(action.isEnabled)
            is GameScreenAction.OnShowWordHelpToggled -> toggleShowWordHelp(action.isEnabled)
            is GameScreenAction.OnSimplifyKeyboardHelpToggled -> toggleSimplifyKeyboardHelp(action.isEnabled)
            GameScreenAction.OnTypoSuggestionDismissed -> dismissTypoSuggestion()
            GameScreenAction.OnCheckClick -> checkAnswer()
            GameScreenAction.OnRetryClick -> repeatLastSession()
            GameScreenAction.OnRandomReviewClick -> repeatRandomStudiedCards()
            GameScreenAction.OnLearnMoreClick -> learnMore()
            GameScreenAction.OnBackToStudentClick -> {
                _effects.value = GameScreenEffect.OpenActiveStudent
            }
            GameScreenAction.OnCloseClick -> {
                _effects.value = GameScreenEffect.CloseGame
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun loadDeckCards() {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading

            if (selectedDeckIds.isEmpty()) {
                _uiState.value = GameUiState.Finished(
                    deckTitle = "Колода",
                    totalCards = 0,
                    correctAnswers = 0,
                )
                return@launch
            }

            val decksById = selectedDeckIds.associateWith { deckId ->
                deckRepository.getDeckById(deckId)
            }
            val deckData = GameDeckData(
                title = buildDeckTitle(selectedDeckIds, decksById),
            )
            val activeStudentId = preferencesRepository.getLastActiveStudentId().orEmpty()
            val activeSymbols = loadStudiedSymbols(
                studentId = activeStudentId,
                studentRepository = studentRepository,
            )
            val isShiftEnabled = loadKeyboardShiftEnabled(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )
            val preventWrongKeyPress = loadPreventWrongKeyPressEnabled(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )
            val allowNeighborTypos = loadAllowNeighborTyposEnabled(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )
            val neighborTypoSensitivity = loadNeighborTypoSensitivity(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )
            val freeNeighborSlipPresses = loadFreeNeighborSlipPresses(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )
            val keyboardPressDelay = loadKeyboardPressDelay(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )
            val hideDigitsOnTightScreen = loadHideDigitsOnTightScreenEnabled(
                studentId = activeStudentId,
                preferencesRepository = preferencesRepository,
            )

            val preparedSession = when (launchMode) {
                GameLaunchMode.Plan -> buildSessionCardsWithPracticeFallback(
                    deckIds = selectedDeckIds,
                    studentId = activeStudentId,
                    flashcardRepository = flashcardRepository,
                    cardProgressRepository = cardProgressRepository,
                    studentPrefsRepository = studentPrefsRepository,
                    dailyLimitIncrease = dailyLimitIncrease,
                )

                GameLaunchMode.RandomLearned -> PreparedSessionCards(
                    cards = buildRandomReviewCards(
                        deckIds = selectedDeckIds,
                        studentId = activeStudentId,
                        flashcardRepository = flashcardRepository,
                        cardProgressRepository = cardProgressRepository,
                        studentPrefsRepository = studentPrefsRepository,
                    ),
                    mode = GameSessionMode.RandomReview,
                )

                GameLaunchMode.RandomAll -> PreparedSessionCards(
                    cards = buildRandomAllCards(
                        deckIds = selectedDeckIds,
                        flashcardRepository = flashcardRepository,
                    ),
                    mode = GameSessionMode.RandomReview,
                )
            }
            saveLastSessionCardsSnapshot(
                studentId = activeStudentId,
                deckIds = selectedDeckIds,
                sessionCards = preparedSession.cards,
                preferencesRepository = preferencesRepository,
            )

            val initialSession = GameSessionData(
                deck = deckData,
                cards = preparedSession.cards,
                studentId = activeStudentId,
                activeSymbols = activeSymbols,
                preventWrongKeyPress = preventWrongKeyPress,
                allowNeighborTypos = allowNeighborTypos,
                neighborTypoSensitivity = neighborTypoSensitivity,
                freeNeighborSlipPresses = freeNeighborSlipPresses,
                keyboardPressDelayMs = keyboardPressDelay.intervalMs,
                isShiftEnabled = isShiftEnabled,
                hideDigitsOnTightScreen = hideDigitsOnTightScreen,
                sessionMode = preparedSession.mode,
                learningStage = initialLearningStage(preparedSession.cards.firstOrNull()),
                isHintVisible = initialHintVisible(preparedSession.cards),
                copySuccessStreak = preparedSession.cards.firstOrNull()?.storedCopySuccessStreak ?: 0,
                cardShownAtEpochMillis = nowMillis(),
                attemptStartedAtEpochMillis = nowMillis(),
                attemptIndex = 1,
            )
            session = initialSession

            _uiState.value = if (preparedSession.cards.isEmpty()) {
                initialSession.toFinishedUiState()
            } else {
                initialSession.toActiveUiState()
            }
        }
    }

    private fun updateAnswer(value: String) {
        val current = session ?: return
        if (current.isFinished || current.feedback != null) return

        val updated = current.copy(
            answerInput = value,
            feedback = null,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun handleKeyboardSymbolPress(symbol: String) {
        val current = session ?: return
        if (current.isFinished || current.feedback != null) return
        val isFastTyping = current.isFastTyping()
        val throttledCurrent = current.consumeKeyboardPressThrottle() ?: return
        session = throttledCurrent

        if (!throttledCurrent.preventWrongKeyPress) {
            updateAnswer(throttledCurrent.answerInput + symbol)
            return
        }

        val expectedSymbol = throttledCurrent.expectedSymbolForPressed(symbol)
        val acceptsPressedSymbol = throttledCurrent.acceptsPressedSymbol(symbol)
        if (acceptsPressedSymbol && expectedSymbol != null) {
            val updated = throttledCurrent.copy(
                answerInput = throttledCurrent.answerInput + symbol,
            )
            session = updated
            _uiState.value = updated.toActiveUiState()
            triggerTypingFeedback(
                symbol = symbol,
                type = TrainingKeyboardFeedbackType.Correct,
            )
        } else if (
            expectedSymbol != null &&
            isNeighborKeyboardSlip(
                referenceText = throttledCurrent.currentCard?.answer.orEmpty(),
                expectedSymbol = expectedSymbol,
                pressedSymbol = symbol,
                sensitivity = NeighborTypoSensitivity.Normal,
            )
        ) {
            val shouldTreatAsSlip = throttledCurrent.allowNeighborTypos &&
                isNeighborKeyboardSlip(
                    referenceText = throttledCurrent.currentCard?.answer.orEmpty(),
                    expectedSymbol = expectedSymbol,
                    pressedSymbol = symbol,
                    sensitivity = throttledCurrent.neighborTypoSensitivity,
                )
            val updated = throttledCurrent.registerFastNeighborTypoIfNeeded(
                isFastTyping = isFastTyping,
            ).let { base ->
                if (shouldTreatAsSlip) {
                    base.copy(
                        slipPressCount = base.slipPressCount + 1,
                    )
                } else {
                    base.copy(
                        wrongPressCount = base.wrongPressCount + 1,
                    )
                }
            }
            session = updated
            _uiState.value = updated.toActiveUiState()
            triggerTypingFeedback(
                symbol = symbol,
                type = if (shouldTreatAsSlip) {
                    TrainingKeyboardFeedbackType.Slip
                } else {
                    TrainingKeyboardFeedbackType.Wrong
                },
            )
            if (!shouldTreatAsSlip) {
                playInvalidKeySoundStub()
            }
        } else {
            val updated = throttledCurrent.copy(
                wrongPressCount = throttledCurrent.wrongPressCount + 1,
            )
            session = updated
            _uiState.value = updated.toActiveUiState()
            triggerTypingFeedback(
                symbol = symbol,
                type = TrainingKeyboardFeedbackType.Wrong,
            )
            playInvalidKeySoundStub()
        }
    }

    private fun handleBackspacePressed() {
        val current = session ?: return
        if (current.isFinished || current.feedback != null) return
        val throttledCurrent = current.consumeKeyboardPressThrottle() ?: return
        val updated = throttledCurrent.copy(
            answerInput = throttledCurrent.answerInput.dropLast(1),
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun updateKeyboardShift(isEnabled: Boolean) {
        val current = session ?: return
        if (current.isFinished || current.isShiftEnabled == isEnabled) return

        val updated = current.copy(isShiftEnabled = isEnabled)
        session = updated
        _uiState.value = updated.toActiveUiState()
        persistKeyboardShiftEnabled(
            studentId = current.studentId,
            isEnabled = isEnabled,
        )
    }

    private fun triggerTypingFeedback(
        symbol: String,
        type: TrainingKeyboardFeedbackType,
    ) {
        keyFeedbackJob?.cancel()
        val current = session ?: return
        val updated = current.copy(
            keyboardFeedbackKey = symbol.normalizedFeedbackKey(),
            keyboardFeedbackType = type,
            inputFeedbackType = type,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()

        keyFeedbackJob = viewModelScope.launch {
            delay(KEY_FEEDBACK_DURATION_MS)
            val actual = session ?: return@launch
            val cleared = actual.copy(
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
            )
            session = cleared
            _uiState.value = if (cleared.isFinished) {
                cleared.toFinishedUiState()
            } else {
                cleared.toActiveUiState()
            }
        }
    }

    private fun playInvalidKeySoundStub() {
        // TODO connect invalid key press sound playback here.
    }

    private fun toggleShowWordHelp(isEnabled: Boolean) {
        val current = session ?: return
        if (current.isFinished || current.feedback != null) return
        if (current.learningStage != TypingLearningStage.Recall) return

        val updated = current.copy(
            usedShowWord = current.usedShowWord || isEnabled,
            isHintVisible = isEnabled,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun toggleInputHintHelp(isEnabled: Boolean) {
        val current = session ?: return
        if (current.isFinished || current.feedback != null) return

        val updated = current.copy(
            usedInputHint = current.usedInputHint || isEnabled,
            isInputHintEnabled = isEnabled,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun toggleSimplifyKeyboardHelp(isEnabled: Boolean) {
        val current = session ?: return
        if (current.isFinished || current.feedback != null) return

        val updated = current.copy(
            usedSimplifiedKeyboard = current.usedSimplifiedKeyboard || isEnabled,
            isSimplifiedKeyboardEnabled = isEnabled,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun dismissTypoSuggestion() {
        val current = session ?: return
        val updated = current.copy(
            showTypoSettingsSuggestion = false,
            hasShownTypoSettingsSuggestion = true,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun checkCopyStageAnswerLocally(current: GameSessionData) {
        val card = current.currentCard ?: return
        val userInput = current.answerInput.canonicalizeOptionalSpacesForExpected(card.answer)
        val isCorrect = userInput.trim().equals(card.answer.trim(), ignoreCase = true)

        if (!isCorrect) {
            handleWrongAnswer(
                current.copy(
                    copySuccessStreak = 0,
                    attemptIndex = current.attemptIndex + 1,
                    lastHintLevel = current.computeCurrentHintLevel(),
                    lastAttemptDurationMs = current.currentAttemptDurationMs(),
                )
            )
            return
        }

        val hintLevel = current.computeCurrentHintLevel()
        val isIdealCopyAttempt = hintLevel == HintLevel.None
        val nextCopySuccessStreak = if (isIdealCopyAttempt) {
            current.copySuccessStreak + 1
        } else {
            0
        }

        val withFeedback = current.copy(
            copySuccessStreak = nextCopySuccessStreak,
            lastHintLevel = hintLevel,
            lastAttemptDurationMs = current.currentAttemptDurationMs(),
            feedback = GameFeedbackData(isCorrect = true),
        )
        session = withFeedback
        _uiState.value = withFeedback.toActiveUiState()

        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)

            val afterCopyAttempt = if (
                nextCopySuccessStreak >= (current.currentCard?.copyStageSuccessThreshold ?: 2)
            ) {
                withFeedback.resetAttempt(
                    learningStage = TypingLearningStage.Recall,
                    isHintVisible = false,
                    copySuccessStreak = 0,
                )
            } else {
                withFeedback.resetAttempt(
                    learningStage = TypingLearningStage.Copy,
                    isHintVisible = true,
                    copySuccessStreak = nextCopySuccessStreak,
                )
            }
            session = afterCopyAttempt
            _uiState.value = afterCopyAttempt.toActiveUiState()
        }
    }

    private fun checkAnswer() {
        val current = session ?: return
        if (current.isFinished) return

        val card = current.currentCard ?: return
        val studentId = current.studentId
        val currentHintLevel = current.computeCurrentHintLevel()
        val currentAttemptDurationMs = current.currentAttemptDurationMs()

        if (studentId.isBlank() || current.sessionMode == GameSessionMode.RandomReview) {
            if (current.learningStage == TypingLearningStage.Copy) {
                checkCopyStageAnswerLocally(current)
                return
            }
            val userInput = current.answerInput.canonicalizeOptionalSpacesForExpected(card.answer)
            val isCorrect = userInput.trim().equals(card.answer.trim(), ignoreCase = true)
            if (isCorrect) {
                handleCorrectAnswer(
                    current.copy(
                        lastHintLevel = currentHintLevel,
                        lastAttemptDurationMs = currentAttemptDurationMs,
                    )
                )
            } else {
                handleWrongAnswer(current)
            }
            return
        }

        viewModelScope.launch {
            val userInput = current.answerInput.canonicalizeOptionalSpacesForExpected(card.answer)
            val submitResult = submitAnswerAndRescheduleUseCase(
                SubmitAnswerCommand(
                    studentId = studentId,
                    cardId = card.id,
                    expectedAnswer = card.answer,
                    userInput = userInput,
                    shownAtEpochMillis = current.cardShownAtEpochMillis,
                    submittedAtEpochMillis = nowMillis(),
                    hintLevel = currentHintLevel.value,
                    wrongPressCount = current.wrongPressCount,
                    durationMs = currentAttemptDurationMs,
                    isRecallStage = current.learningStage == TypingLearningStage.Recall,
                    copyStageSuccessThreshold = card.copyStageSuccessThreshold,
                )
            )

            val actual = session ?: return@launch
            if (actual.currentCard?.id != card.id) return@launch

            if (actual.learningStage == TypingLearningStage.Copy) {
                val withProgress = actual.copy(
                    copySuccessStreak = submitResult.updatedProgress.copySuccessStreak,
                    lastHintLevel = currentHintLevel,
                    lastAttemptDurationMs = currentAttemptDurationMs,
                )
                if (!submitResult.isCorrect) {
                    handleWrongAnswer(withProgress.copy(attemptIndex = actual.attemptIndex + 1))
                    return@launch
                }

                val withFeedback = withProgress.copy(feedback = GameFeedbackData(isCorrect = true))
                session = withFeedback
                _uiState.value = withFeedback.toActiveUiState()

                feedbackJob?.cancel()
                feedbackJob = viewModelScope.launch {
                    delay(FEEDBACK_DURATION_MS)

                    val afterCopyAttempt = if (
                        submitResult.updatedProgress.copySuccessStreak >= card.copyStageSuccessThreshold
                    ) {
                        withFeedback.resetAttempt(
                            learningStage = TypingLearningStage.Recall,
                            isHintVisible = false,
                            copySuccessStreak = submitResult.updatedProgress.copySuccessStreak,
                        )
                    } else {
                        withFeedback.resetAttempt(
                            learningStage = TypingLearningStage.Copy,
                            isHintVisible = true,
                            copySuccessStreak = submitResult.updatedProgress.copySuccessStreak,
                        )
                    }
                    session = afterCopyAttempt
                    _uiState.value = afterCopyAttempt.toActiveUiState()
                }
            } else if (!submitResult.isCorrect) {
                handleWrongAnswer(
                    actual.copy(
                        attemptIndex = actual.attemptIndex + 1,
                        lastHintLevel = currentHintLevel,
                        lastAttemptDurationMs = currentAttemptDurationMs,
                    ),
                )
            } else {
                handleCorrectAnswer(
                    actual.copy(
                        lastHintLevel = currentHintLevel,
                        lastAttemptDurationMs = currentAttemptDurationMs,
                    )
                )
            }
        }
    }

    private fun handleCorrectAnswer(current: GameSessionData) {
        feedbackJob?.cancel()
        val currentCard = current.currentCard ?: return
        val updatedStudiedSymbols = current.activeSymbols + currentCard.extractKeyboardSymbols()
        persistStudiedSymbols(
            studentId = current.studentId,
            studiedSymbols = updatedStudiedSymbols,
        )

        val withFeedback = current.copy(
            activeSymbols = updatedStudiedSymbols,
            isHintVisible = false,
            feedback = GameFeedbackData(isCorrect = true),
        )
        session = withFeedback
        _uiState.value = withFeedback.toActiveUiState()

        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)

            val nextIndex = withFeedback.currentIndex + 1
            val nextCard = withFeedback.cards.getOrNull(nextIndex)
            val nextStage = initialLearningStage(nextCard)
            val progressed = withFeedback.copy(
                currentIndex = nextIndex,
                correctAnswers = withFeedback.correctAnswers + 1,
                learningStage = nextStage,
                answerInput = "",
                isHintVisible = nextStage == TypingLearningStage.Copy,
                isInputHintEnabled = false,
                copySuccessStreak = nextCard?.storedCopySuccessStreak ?: 0,
                wrongPressCount = 0,
                slipPressCount = 0,
                fastNeighborTypoCount = 0,
                usedInputHint = false,
                usedShowWord = false,
                usedSimplifiedKeyboard = false,
                isSimplifiedKeyboardEnabled = false,
                showTypoSettingsSuggestion = false,
                feedback = null,
                cardShownAtEpochMillis = nowMillis(),
                attemptStartedAtEpochMillis = nowMillis(),
                attemptIndex = 1,
                lastHintLevel = null,
                lastAttemptDurationMs = null,
                lastHandledKeyPressAtEpochMillis = 0L,
            )
            session = progressed

            _uiState.value = if (progressed.isFinished) {
                progressed.toFinishedUiState()
            } else {
                progressed.toActiveUiState()
            }
        }
    }

    private fun handleWrongAnswer(current: GameSessionData) {
        feedbackJob?.cancel()

        val withFeedback = current.copy(
            isHintVisible = current.isHintVisible || current.learningStage == TypingLearningStage.Copy,
            feedback = GameFeedbackData(isCorrect = false),
        )
        session = withFeedback
        _uiState.value = withFeedback.toActiveUiState()

        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)
            val cleared = withFeedback.copy(feedback = null)
            session = cleared
            _uiState.value = cleared.toActiveUiState()
        }
    }

    private fun repeatLastSession() {
        feedbackJob?.cancel()

        val current = session ?: return
        viewModelScope.launch {
            val restoredCards = restoreLastSessionCards(
                studentId = current.studentId,
                deckIds = selectedDeckIds,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
                preferencesRepository = preferencesRepository,
            ).ifEmpty { current.cards }

            val restarted = restartSessionWithCards(
                current = current,
                cards = restoredCards,
            )
            session = restarted

            _uiState.value = if (restarted.cards.isEmpty()) {
                restarted.toFinishedUiState()
            } else {
                restarted.toActiveUiState()
            }
        }
    }

    private fun repeatRandomStudiedCards() {
        feedbackJob?.cancel()

        val current = session ?: return
        viewModelScope.launch {
            val randomReviewCards = buildRandomReviewCards(
                deckIds = selectedDeckIds,
                studentId = current.studentId,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
            ).ifEmpty {
                current.cards.shuffled(Random(nowMillis())).take(RANDOM_REVIEW_LIMIT)
            }

            saveLastSessionCardsSnapshot(
                studentId = current.studentId,
                deckIds = selectedDeckIds,
                sessionCards = randomReviewCards,
                preferencesRepository = preferencesRepository,
            )

            val restarted = restartSessionWithCards(
                current = current,
                cards = randomReviewCards,
                sessionMode = GameSessionMode.RandomReview,
            )
            session = restarted
            _uiState.value = if (restarted.cards.isEmpty()) {
                restarted.toFinishedUiState()
            } else {
                restarted.toActiveUiState()
            }
        }
    }

    private fun learnMore() {
        feedbackJob?.cancel()

        val current = session ?: return
        viewModelScope.launch {
            val learnMoreStep = resolveLearnMoreStep(current.studentId)
            dailyLimitIncrease += learnMoreStep

            val preparedSession = buildSessionCardsWithPracticeFallback(
                deckIds = selectedDeckIds,
                studentId = current.studentId,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
                dailyLimitIncrease = dailyLimitIncrease,
            )
            saveLastSessionCardsSnapshot(
                studentId = current.studentId,
                deckIds = selectedDeckIds,
                sessionCards = preparedSession.cards,
                preferencesRepository = preferencesRepository,
            )

            val restarted = restartSessionWithCards(
                current = current,
                cards = preparedSession.cards,
                sessionMode = preparedSession.mode,
            )
            session = restarted
            _uiState.value = if (restarted.cards.isEmpty()) {
                restarted.toFinishedUiState()
            } else {
                restarted.toActiveUiState()
            }
        }
    }

    private suspend fun resolveLearnMoreStep(studentId: String): Int {
        if (studentId.isBlank()) return DEFAULT_LEARN_MORE_STEP
        return runCatching { studentPrefsRepository.getPrefs(studentId).learnMoreStep }
            .getOrDefault(DEFAULT_LEARN_MORE_STEP)
            .coerceAtLeast(1)
    }

    private fun persistStudiedSymbols(
        studentId: String,
        studiedSymbols: Set<String>,
    ) {
        if (studentId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                studentRepository.updateActiveLetters(
                    id = studentId,
                    activeLetters = studiedSymbols
                        .mapNotNull { symbol -> symbol.singleOrNull() }
                        .joinToString(separator = ""),
                )
            }
        }
    }

    private fun persistKeyboardShiftEnabled(
        studentId: String,
        isEnabled: Boolean,
    ) {
        if (studentId.isBlank()) return
        runCatching {
            preferencesRepository.setKeyboardShiftEnabled(
                studentId = studentId,
                isEnabled = isEnabled,
            )
        }
    }

    private fun restartSessionWithCards(
        current: GameSessionData,
        cards: List<GameCardData>,
        sessionMode: GameSessionMode = current.sessionMode,
    ): GameSessionData {
        val firstCard = cards.firstOrNull()
        val initialStage = initialLearningStage(firstCard)
        return current.copy(
            cards = cards,
            sessionMode = sessionMode,
            currentIndex = 0,
            correctAnswers = 0,
            learningStage = initialStage,
            answerInput = "",
            isHintVisible = initialStage == TypingLearningStage.Copy,
            copySuccessStreak = firstCard?.storedCopySuccessStreak ?: 0,
            wrongPressCount = 0,
            slipPressCount = 0,
            fastNeighborTypoCount = 0,
            usedShowWord = false,
            usedSimplifiedKeyboard = false,
            isSimplifiedKeyboardEnabled = false,
            showTypoSettingsSuggestion = false,
            keyboardFeedbackKey = null,
            keyboardFeedbackType = null,
            inputFeedbackType = null,
            feedback = null,
            cardShownAtEpochMillis = nowMillis(),
            attemptStartedAtEpochMillis = nowMillis(),
            attemptIndex = 1,
            lastHintLevel = null,
            lastAttemptDurationMs = null,
            lastHandledKeyPressAtEpochMillis = 0L,
        )
    }
}

private data class GameDeckData(
    val title: String,
)

private data class GameCardData(
    val deckId: String,
    val id: String,
    val answer: String,
    val imagePath: String?,
    val showHintInitially: Boolean = false,
    val copyStageSuccessThreshold: Int = 2,
    val storedCopySuccessStreak: Int = 0,
)

private enum class GameSessionMode {
    Srs,
    RandomReview,
}

private data class GameSessionData(
    val deck: GameDeckData,
    val cards: List<GameCardData>,
    val studentId: String,
    val activeSymbols: Set<String>,
    val preventWrongKeyPress: Boolean,
    val allowNeighborTypos: Boolean,
    val neighborTypoSensitivity: NeighborTypoSensitivity,
    val freeNeighborSlipPresses: Int,
    val keyboardPressDelayMs: Long,
    val isShiftEnabled: Boolean,
    val hideDigitsOnTightScreen: Boolean,
    val learningStage: TypingLearningStage,
    val keyboardFeedbackKey: String? = null,
    val keyboardFeedbackType: TrainingKeyboardFeedbackType? = null,
    val inputFeedbackType: TrainingKeyboardFeedbackType? = null,
    val sessionMode: GameSessionMode,
    val currentIndex: Int = 0,
    val correctAnswers: Int = 0,
    val answerInput: String = "",
    val isHintVisible: Boolean = false,
    val isInputHintEnabled: Boolean = false,
    val isSimplifiedKeyboardEnabled: Boolean = false,
    val cardShownAtEpochMillis: Long,
    val attemptStartedAtEpochMillis: Long,
    val attemptIndex: Int,
    val copySuccessStreak: Int = 0,
    val wrongPressCount: Int = 0,
    val slipPressCount: Int = 0,
    val fastNeighborTypoCount: Int = 0,
    val usedInputHint: Boolean = false,
    val usedShowWord: Boolean = false,
    val usedSimplifiedKeyboard: Boolean = false,
    val showTypoSettingsSuggestion: Boolean = false,
    val hasShownTypoSettingsSuggestion: Boolean = false,
    val lastHintLevel: HintLevel? = null,
    val lastAttemptDurationMs: Long? = null,
    val lastHandledKeyPressAtEpochMillis: Long = 0L,
    val feedback: GameFeedbackData? = null,
) {
    val currentCard: GameCardData?
        get() = cards.getOrNull(currentIndex)

    val isFinished: Boolean
        get() = currentIndex >= cards.size

    fun expectedSymbolForPressed(pressedSymbol: String): String? {
        val answer = currentCard?.answer ?: return null
        val alignment = answerInputAlignment(
            answerInput = answerInput,
            expectedAnswer = answer,
        )
        val expectedSymbol = answer.getOrNull(alignment.nextExpectedIndex)?.toString() ?: return null
        if (expectedSymbol != " " || pressedSymbol == " ") return expectedSymbol
        return answer
            .substring(alignment.nextExpectedIndex)
            .firstOrNull { !it.isWhitespace() }
            ?.toString()
    }

    fun acceptsPressedSymbol(pressedSymbol: String): Boolean {
        val answer = currentCard?.answer ?: return false
        val alignment = answerInputAlignment(
            answerInput = answerInput,
            expectedAnswer = answer,
        )
        val nextIndex = alignment.nextExpectedIndex
        val nextExpectedChar = answer.getOrNull(nextIndex) ?: return false
        if (!nextExpectedChar.isWhitespace()) {
            return pressedSymbol.matchesExpectedSymbol(nextExpectedChar.toString())
        }
        if (pressedSymbol == " ") return true
        val nextLetter = answer
            .substring(nextIndex)
            .firstOrNull { !it.isWhitespace() }
            ?: return false
        return pressedSymbol.matchesExpectedSymbol(nextLetter.toString())
    }
}

private data class GameFeedbackData(
    val isCorrect: Boolean,
)

private fun GameSessionData.computeCurrentHintLevel(): HintLevel {
    return computeHintLevel(
        TypingAttemptMetrics(
            wrongPressCount = wrongPressCount,
            slipPressCount = slipPressCount,
            freeSlipPresses = freeNeighborSlipPresses,
            usedInputHint = usedInputHint,
            usedShowWord = usedShowWord,
            usedSimplifiedKeyboard = usedSimplifiedKeyboard,
        )
    )
}

private fun GameSessionData.currentAttemptDurationMs(): Long {
    return (nowMillis() - attemptStartedAtEpochMillis).coerceAtLeast(0L)
}

private fun GameSessionData.isFastTyping(): Boolean {
    if (lastHandledKeyPressAtEpochMillis == 0L) return false
    return nowMillis() - lastHandledKeyPressAtEpochMillis <= FAST_TYPING_INTERVAL_MS
}

private fun GameSessionData.consumeKeyboardPressThrottle(): GameSessionData? {
    val now = nowMillis()
    if (now - lastHandledKeyPressAtEpochMillis < keyboardPressDelayMs) return null
    return copy(lastHandledKeyPressAtEpochMillis = now)
}

private fun GameSessionData.resetAttempt(
    learningStage: TypingLearningStage = this.learningStage,
    isHintVisible: Boolean = this.isHintVisible,
    copySuccessStreak: Int = this.copySuccessStreak,
): GameSessionData {
    return copy(
        learningStage = learningStage,
        answerInput = "",
        isHintVisible = isHintVisible,
        isInputHintEnabled = false,
        keyboardFeedbackKey = null,
        keyboardFeedbackType = null,
        inputFeedbackType = null,
        wrongPressCount = 0,
        slipPressCount = 0,
        fastNeighborTypoCount = 0,
        usedInputHint = false,
        usedShowWord = false,
        usedSimplifiedKeyboard = false,
        isSimplifiedKeyboardEnabled = false,
        showTypoSettingsSuggestion = false,
        feedback = null,
        cardShownAtEpochMillis = nowMillis(),
        attemptStartedAtEpochMillis = nowMillis(),
        attemptIndex = 1,
        copySuccessStreak = copySuccessStreak,
        lastHintLevel = null,
        lastAttemptDurationMs = null,
        lastHandledKeyPressAtEpochMillis = 0L,
    )
}

private fun GameSessionData.registerFastNeighborTypoIfNeeded(
    isFastTyping: Boolean,
): GameSessionData {
    if (!isFastTyping || hasShownTypoSettingsSuggestion) return this
    val nextFastNeighborTypoCount = fastNeighborTypoCount + 1
    return copy(
        fastNeighborTypoCount = nextFastNeighborTypoCount,
        showTypoSettingsSuggestion = nextFastNeighborTypoCount >= FAST_NEIGHBOR_TYPO_SUGGESTION_THRESHOLD,
    )
}

private suspend fun buildSessionCards(
    deckIds: List<String>,
    studentId: String,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
    dailyLimitIncrease: Int = 0,
): List<GameCardData> {
    val random = Random(nowMillis())
    val cardsByDeck = deckIds.associateWith { deckId ->
        flashcardRepository.getFlashcardsByDeckId(deckId)
            .shuffled(random)
            .map { card ->
                GameCardData(
                    deckId = deckId,
                    id = card.id,
                    answer = card.name,
                    imagePath = card.imageUrl.ifBlank { null },
                    showHintInitially = false,
                    copyStageSuccessThreshold = 2,
                )
            }
    }

    if (studentId.isBlank()) {
        return takeRoundRobinByDeck(
            cardsByDeck = cardsByDeck,
            limit = cardsByDeck.values.sumOf { it.size },
        )
    }

    val prefs = runCatching { studentPrefsRepository.getPrefs(studentId) }.getOrNull()
        ?: return takeRoundRobinByDeck(
            cardsByDeck = cardsByDeck,
            limit = cardsByDeck.values.sumOf { it.size },
        )
    val normalizedDailyIncrease = dailyLimitIncrease.coerceAtLeast(0)
    val reviewLimit = prefs.reviewsPerSession.coerceAtLeast(0) + normalizedDailyIncrease
    val newLimit = prefs.newCardsPerSession.coerceAtLeast(0) + normalizedDailyIncrease
    val guidedHintThreshold = prefs.guidedHintSuccessThreshold
        .coerceIn(MIN_GUIDED_HINT_THRESHOLD, MAX_GUIDED_HINT_THRESHOLD)
    val progressByCardId = cardProgressRepository.observeProgress(studentId)
        .first()
        .associateBy { it.cardId }
    val now = nowMillis()

    val newByDeck = mutableMapOf<String, MutableList<GameCardData>>()
    val reviewByDeck = mutableMapOf<String, MutableList<GameCardData>>()
    cardsByDeck.forEach { (deckId, cards) ->
        cards.forEach { card ->
            val progress = progressByCardId[card.id]
            val cardWithHintMode = card.copy(
                showHintInitially = shouldShowHintInitially(
                    progress = progress,
                    guidedHintThreshold = guidedHintThreshold,
                ),
                copyStageSuccessThreshold = guidedHintThreshold.coerceAtLeast(1),
                storedCopySuccessStreak = progress?.copySuccessStreak ?: 0,
            )
            if (progress == null || cardWithHintMode.showHintInitially) {
                newByDeck.getOrPut(deckId) { mutableListOf() }.add(cardWithHintMode)
            } else if (progress.dueAtEpochMillis <= now) {
                reviewByDeck.getOrPut(deckId) { mutableListOf() }.add(cardWithHintMode)
            }
        }
    }

    val selectedReview = takeRoundRobinByDeck(
        cardsByDeck = reviewByDeck,
        limit = reviewLimit,
    )
    val selectedNew = selectBalancedNewCardsByDeck(
        newCardsByDeck = newByDeck,
        newLimit = newLimit,
    )

    return interleaveReviewAndNewCards(
        reviewCards = selectedReview,
        newCards = selectedNew,
        reviewToNewRatio = REVIEW_TO_NEW_RATIO,
    )
}

private fun saveLastSessionCardsSnapshot(
    studentId: String,
    deckIds: List<String>,
    sessionCards: List<GameCardData>,
    preferencesRepository: PreferencesRepository,
) {
    val cardIds = sessionCards.map { it.id }
    if (cardIds.isEmpty()) return
    preferencesRepository.setLastSessionCardIds(
        studentId = studentId,
        deckIds = deckIds,
        cardIds = cardIds,
    )
}

private suspend fun restoreLastSessionCards(
    studentId: String,
    deckIds: List<String>,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
    preferencesRepository: PreferencesRepository,
): List<GameCardData> {
    val savedCardIds = preferencesRepository.getLastSessionCardIds(
        studentId = studentId,
        deckIds = deckIds,
    ).orEmpty()
    if (savedCardIds.isEmpty()) return emptyList()

    val progressByCardId: Map<String, CardProgress> = if (studentId.isBlank()) {
        emptyMap()
    } else {
        cardProgressRepository.observeProgress(studentId)
            .first()
            .associateBy { it.cardId }
    }
    val guidedHintThreshold = if (studentId.isBlank()) {
        MIN_GUIDED_HINT_THRESHOLD
    } else {
        runCatching { studentPrefsRepository.getPrefs(studentId).guidedHintSuccessThreshold }
            .getOrDefault(2)
            .coerceIn(MIN_GUIDED_HINT_THRESHOLD, MAX_GUIDED_HINT_THRESHOLD)
    }

    val cardsById = buildMap {
        deckIds.forEach { deckId ->
            flashcardRepository.getFlashcardsByDeckId(deckId).forEach { card ->
                val progress = progressByCardId[card.id]
                put(
                    card.id,
                    GameCardData(
                        deckId = deckId,
                        id = card.id,
                        answer = card.name,
                        imagePath = card.imageUrl.ifBlank { null },
                        showHintInitially = shouldShowHintInitially(
                            progress = progress,
                            guidedHintThreshold = guidedHintThreshold,
                        ),
                        copyStageSuccessThreshold = guidedHintThreshold.coerceAtLeast(1),
                        storedCopySuccessStreak = progress?.copySuccessStreak ?: 0,
                    )
                )
            }
        }
    }
    return savedCardIds.mapNotNull { cardId -> cardsById[cardId] }
}

private suspend fun buildRandomReviewCards(
    deckIds: List<String>,
    studentId: String,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
): List<GameCardData> {
    if (studentId.isBlank()) return emptyList()

    val progressByCardId = cardProgressRepository.observeProgress(studentId)
        .first()
        .associateBy { it.cardId }
    val guidedHintThreshold = runCatching { studentPrefsRepository.getPrefs(studentId).guidedHintSuccessThreshold }
        .getOrDefault(2)
        .coerceIn(MIN_GUIDED_HINT_THRESHOLD, MAX_GUIDED_HINT_THRESHOLD)
    val todayStartMillis = localStartOfDayMillis()
    val random = Random(nowMillis())

    val eligibleCards = buildList {
        deckIds.forEach { deckId ->
            flashcardRepository.getFlashcardsByDeckId(deckId).forEach { flashcard ->
                val progress = progressByCardId[flashcard.id] ?: return@forEach
                if (progress.level <= 0) return@forEach

                add(
                    GameCardWithProgress(
                        card = GameCardData(
                            deckId = deckId,
                            id = flashcard.id,
                            answer = flashcard.name,
                            imagePath = flashcard.imageUrl.ifBlank { null },
                            showHintInitially = shouldShowHintInitially(
                                progress = progress,
                                guidedHintThreshold = guidedHintThreshold,
                            ),
                            copyStageSuccessThreshold = guidedHintThreshold.coerceAtLeast(1),
                            storedCopySuccessStreak = progress.copySuccessStreak,
                        ),
                        progress = progress,
                    )
                )
            }
        }
    }

    val reviewedToday = eligibleCards
        .filter { candidate ->
            val lastReviewed = candidate.progress.lastReviewedAtEpochMillis
            lastReviewed != null && lastReviewed >= todayStartMillis
        }
        .shuffled(random)
        .take(RANDOM_REVIEW_LIMIT)

    val reviewedTodayIds = reviewedToday.mapTo(mutableSetOf()) { it.card.id }
    val otherStudied = eligibleCards
        .filterNot { it.card.id in reviewedTodayIds }
        .shuffled(random)
        .take((RANDOM_REVIEW_LIMIT - reviewedToday.size).coerceAtLeast(0))

    return (reviewedToday + otherStudied)
        .map { it.card }
        .shuffled(random)
}

private suspend fun buildRandomAllCards(
    deckIds: List<String>,
    flashcardRepository: FlashcardRepository,
): List<GameCardData> {
    val random = Random(nowMillis())
    return deckIds
        .flatMap { deckId ->
            flashcardRepository.getFlashcardsByDeckId(deckId).map { flashcard ->
                GameCardData(
                    deckId = deckId,
                    id = flashcard.id,
                    answer = flashcard.name,
                    imagePath = flashcard.imageUrl.ifBlank { null },
                    showHintInitially = false,
                    copyStageSuccessThreshold = 2,
                )
            }
        }
        .shuffled(random)
        .take(RANDOM_REVIEW_LIMIT)
}

private data class GameCardWithProgress(
    val card: GameCardData,
    val progress: CardProgress,
)

private data class PreparedSessionCards(
    val cards: List<GameCardData>,
    val mode: GameSessionMode,
)

private suspend fun buildSessionCardsWithPracticeFallback(
    deckIds: List<String>,
    studentId: String,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
    dailyLimitIncrease: Int = 0,
): PreparedSessionCards {
    val srsCards = buildSessionCards(
        deckIds = deckIds,
        studentId = studentId,
        flashcardRepository = flashcardRepository,
        cardProgressRepository = cardProgressRepository,
        studentPrefsRepository = studentPrefsRepository,
        dailyLimitIncrease = dailyLimitIncrease,
    )
    if (srsCards.isNotEmpty()) {
        return PreparedSessionCards(
            cards = srsCards,
            mode = GameSessionMode.Srs,
        )
    }

    val practiceCards = buildRandomReviewCards(
        deckIds = deckIds,
        studentId = studentId,
        flashcardRepository = flashcardRepository,
        cardProgressRepository = cardProgressRepository,
        studentPrefsRepository = studentPrefsRepository,
    )
    if (practiceCards.isNotEmpty()) {
        return PreparedSessionCards(
            cards = practiceCards,
            mode = GameSessionMode.RandomReview,
        )
    }

    return PreparedSessionCards(
        cards = emptyList(),
        mode = GameSessionMode.Srs,
    )
}

private fun buildDeckTitle(
    deckIds: List<String>,
    decksById: Map<String, Deck?>,
): String {
    if (deckIds.size == 1) {
        val singleDeck = decksById[deckIds.first()]
        return singleDeck?.name?.ifBlank { "Колода" } ?: "Колода"
    }
    return "Смешанная сессия"
}

private fun GameSessionData.toActiveUiState(): GameUiState.Active {
    val card = requireNotNull(currentCard)
    val activeKeyboardSymbols = if (isSimplifiedKeyboardEnabled) {
        card.extractKeyboardSymbols()
    } else {
        activeSymbols + card.extractKeyboardSymbols()
    }
    return GameUiState.Active(
        deckTitle = deck.title,
        currentCard = CardUi(
            id = card.id,
            answer = card.answer,
            imagePath = card.imagePath,
        ),
        cardIndex = currentIndex + 1,
        totalCards = cards.size,
        studentId = studentId,
        isPracticeMode = sessionMode == GameSessionMode.RandomReview,
        learningStage = learningStage,
        activeSymbols = activeKeyboardSymbols,
        preventWrongKeyPress = preventWrongKeyPress,
        allowNeighborTypos = allowNeighborTypos,
        isShiftEnabled = isShiftEnabled,
        hideDigitsOnTightScreen = hideDigitsOnTightScreen,
        keyboardFeedbackKey = keyboardFeedbackKey,
        keyboardFeedbackType = keyboardFeedbackType,
        inputFeedbackType = inputFeedbackType,
        answerInput = answerInput,
        isHintVisible = isHintVisible,
        isInputHintEnabled = isInputHintEnabled,
        isSimplifiedKeyboardEnabled = isSimplifiedKeyboardEnabled,
        copySuccessStreak = copySuccessStreak,
        wrongPressCount = wrongPressCount,
        slipPressCount = slipPressCount,
        usedInputHint = usedInputHint,
        usedShowWord = usedShowWord,
        usedSimplifiedKeyboard = usedSimplifiedKeyboard,
        showTypoSettingsSuggestion = showTypoSettingsSuggestion,
        lastHintLevel = lastHintLevel,
        feedback = feedback?.toUi(),
    )
}

private suspend fun loadStudiedSymbols(
    studentId: String,
    studentRepository: StudentRepository,
): Set<String> {
    if (studentId.isBlank()) return emptySet()
    return studentRepository.getActiveLettersById(studentId)
        .orEmpty()
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .map { it.toString() }
        .toSet()
}

private fun loadKeyboardShiftEnabled(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): Boolean {
    if (studentId.isBlank()) return false
    return preferencesRepository.getKeyboardShiftEnabled(studentId) == true
}

private fun loadPreventWrongKeyPressEnabled(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): Boolean {
    if (studentId.isBlank()) return true
    return preferencesRepository.getPreventWrongKeyPressEnabled(studentId) ?: true
}

private fun loadAllowNeighborTyposEnabled(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): Boolean {
    if (studentId.isBlank()) return true
    return preferencesRepository.getAllowNeighborTyposEnabled(studentId) ?: true
}

private fun loadNeighborTypoSensitivity(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): NeighborTypoSensitivity {
    if (studentId.isBlank()) return NeighborTypoSensitivity.Normal
    return preferencesRepository.getNeighborTypoSensitivity(studentId)
        ?.takeIf { it != NeighborTypoSensitivity.Strict }
        ?: NeighborTypoSensitivity.Normal
}

private fun loadFreeNeighborSlipPresses(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): Int {
    if (studentId.isBlank()) return DEFAULT_FREE_NEIGHBOR_SLIP_PRESSES
    return preferencesRepository.getFreeNeighborSlipPresses(studentId)
        ?.coerceIn(0, 3)
        ?: DEFAULT_FREE_NEIGHBOR_SLIP_PRESSES
}

private fun loadKeyboardPressDelay(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): KeyboardPressDelay {
    if (studentId.isBlank()) return KeyboardPressDelay.Normal
    return preferencesRepository.getKeyboardPressDelay(studentId)
        ?: KeyboardPressDelay.Normal
}

private fun loadHideDigitsOnTightScreenEnabled(
    studentId: String,
    preferencesRepository: PreferencesRepository,
): Boolean {
    if (studentId.isBlank()) return true
    return preferencesRepository.getHideDigitsOnTightScreenEnabled(studentId) ?: true
}

private data class AnswerInputAlignment(
    val nextExpectedIndex: Int,
)

private fun answerInputAlignment(
    answerInput: String,
    expectedAnswer: String,
): AnswerInputAlignment {
    var expectedIndex = 0
    var inputIndex = 0

    while (expectedIndex < expectedAnswer.length && inputIndex < answerInput.length) {
        val expectedChar = expectedAnswer[expectedIndex]
        val inputChar = answerInput[inputIndex]

        when {
            expectedChar.isWhitespace() && inputChar.isWhitespace() -> {
                expectedIndex++
                inputIndex++
            }
            expectedChar.isWhitespace() -> {
                expectedIndex++
            }
            inputChar.toString().matchesExpectedSymbol(expectedChar.toString()) -> {
                expectedIndex++
                inputIndex++
            }
            else -> break
        }
    }

    return AnswerInputAlignment(
        nextExpectedIndex = expectedIndex,
    )
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

private fun String.matchesExpectedSymbol(expectedSymbol: String): Boolean {
    return lowercase() == expectedSymbol.lowercase()
}

private fun String.normalizedFeedbackKey(): String {
    return lowercase()
}

private fun GameCardData.extractKeyboardSymbols(): Set<String> {
    return answer
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .map { it.toString() }
        .toSet()
}

private fun GameSessionData.toFinishedUiState(): GameUiState.Finished {
    return GameUiState.Finished(
        deckTitle = deck.title,
        totalCards = cards.size,
        correctAnswers = correctAnswers,
    )
}

private fun GameFeedbackData.toUi(): FeedbackUi {
    return if (isCorrect) {
        FeedbackUi(
            message = "Верно!",
            emoji = "🥳",
        )
    } else {
        FeedbackUi(
            message = "Неверно!",
            emoji = "😞",
        )
    }
}

private fun initialHintVisible(cards: List<GameCardData>): Boolean {
    return cards.firstOrNull()?.showHintInitially == true
}

private fun initialLearningStage(card: GameCardData?): TypingLearningStage {
    return if (card?.showHintInitially == true) {
        TypingLearningStage.Copy
    } else {
        TypingLearningStage.Recall
    }
}

private fun shouldShowHintInitially(
    progress: CardProgress?,
    guidedHintThreshold: Int,
): Boolean {
    if (guidedHintThreshold <= 0) return false
    if (progress == null) return true
    return progress.copySuccessStreak < guidedHintThreshold
}
