package com.cerebus.fairy_tales.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.utils.nowMillis
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.customkeyboard.isNeighborKeyboardSlip
import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val DefaultFeedbackDurationMillis = 1_000L
private const val CompletionFeedbackDurationMillis = 2_000L
private const val FairyTaleCompletionFanfarePath = "files/fanfare.mp3"
private const val FairyTaleCompletionFanfareFileName = "fanfare.mp3"

class FairyTalesViewModel(
    fairyTaleId: String,
    private val preferencesRepository: PreferencesRepository,
    private val studentRepository: StudentRepository,
) : ViewModel() {

    var state by mutableStateOf(
        FairyTalesCatalog.findById(fairyTaleId)?.toUiState()
            ?: FairyTalesCatalog.items.first().toUiState()
    )
        private set

    private val _effects = MutableSharedFlow<FairyTalesEffect>(
        extraBufferCapacity = 1,
    )
    val effects: SharedFlow<FairyTalesEffect> = _effects.asSharedFlow()

    private var nextPlaybackToken = 0L
    private var activePlaybackToken: Long? = null
    private var studentId: String = ""
    private var studiedSymbols: Set<String> = emptySet()
    private var preventWrongKeyPress: Boolean = true
    private var allowNeighborTypos: Boolean = true
    private var neighborTypoSensitivity: NeighborTypoSensitivity = NeighborTypoSensitivity.Strict
    private var keyboardPressDelayMs: Long = KeyboardPressDelay.Normal.intervalMs
    private var lastHandledKeyPressAtEpochMillis: Long = 0L
    private var keyFeedbackJob: Job? = null
    private var feedbackJob: Job? = null
    private var playbackCompletionJob: Job? = null
    private var animationTransitionJob: Job? = null
    private var animationCycleStartedAtEpochMillis: Long = nowMillis()
    private var hasHandledScreenReady = false

    init {
        viewModelScope.launch {
            studentId = preferencesRepository.getLastActiveStudentId().orEmpty()
            studiedSymbols = loadStudiedSymbols(studentId)
            preventWrongKeyPress = preferencesRepository.getPreventWrongKeyPressEnabled(studentId) ?: true
            allowNeighborTypos = preferencesRepository.getAllowNeighborTyposEnabled(studentId) ?: true
            neighborTypoSensitivity = preferencesRepository.getNeighborTypoSensitivity(studentId)
                ?: NeighborTypoSensitivity.Strict
            keyboardPressDelayMs = (preferencesRepository.getKeyboardPressDelay(studentId)
                ?: KeyboardPressDelay.Normal).intervalMs
            applyState(
                state.copy(
                studentId = studentId,
                isShiftEnabled = preferencesRepository.getKeyboardShiftEnabled(studentId) == true,
                isInputHintEnabled = preferencesRepository.getGalleryInputHintEnabled(studentId) == true,
                isSimplifiedKeyboardEnabled = preferencesRepository
                    .getFairyTalesSimplifiedKeyboardEnabled(studentId) ?: true,
                hideDigitsOnTightScreen = preferencesRepository.getHideDigitsOnTightScreenEnabled(studentId) ?: true,
                )
            )
            refreshActiveSymbols()
        }
    }

    fun onScreenReady() {
        if (hasHandledScreenReady) return
        hasHandledScreenReady = true
        startCurrentLinePlaybackIfNeeded()
    }

    fun onStoryPlaybackStarted(
        playbackToken: Long,
        audioDurationMillis: Long?,
    ) {
        if (activePlaybackToken != playbackToken) return
        val currentLine = state.currentStoryLine ?: return
        val playbackVisual = currentLine.playbackVisual
        val playbackDurationMillis = (audioDurationMillis ?: 0L).coerceAtLeast(0L)

        applyState(
            state.copy(
                visualState = FairyTaleVisualState.Playback(
                    content = playbackVisual,
                    playbackToken = playbackToken,
                ),
            )
        )

        playbackCompletionJob?.cancel()
        if (playbackDurationMillis <= 0L) {
            onAnimationCompleted(playbackToken)
            return
        }

        playbackCompletionJob = viewModelScope.launch {
            delay(playbackDurationMillis)
            onAnimationCompleted(playbackToken)
        }
    }

    fun onAnimationCompleted(playbackToken: Long) {
        val currentVisualState = state.visualState as? FairyTaleVisualState.Playback ?: return
        if (currentVisualState.playbackToken != playbackToken || activePlaybackToken != playbackToken) return

        playbackCompletionJob?.cancel()
        activePlaybackToken = null
        if (state.lineProgressMode == FairyTaleLineProgressMode.PlaybackThenInput) {
            transitionAfterCurrentAnimationCycle {
                applyState(
                    state.copy(
                        isStoryPlaybackInProgress = false,
                        visualState = FairyTaleVisualState.Waiting(
                            waitingVisualFor(
                                visualScheme = state.visualScheme,
                                waitingVisualMode = state.waitingVisualMode,
                                lineIndex = state.currentLineIndex,
                                linePlaybackVisual = state.currentStoryLine?.playbackVisual
                                    ?: FairyTaleVisualContent.None,
                            )
                        ),
                    )
                )
            }
            return
        }

        val nextLineIndex = state.currentLineIndex + 1
        if (nextLineIndex < state.storyLines.size) {
            transitionAfterCurrentAnimationCycle {
                openStoryLine(nextLineIndex)
            }
            return
        }

            transitionAfterCurrentAnimationCycle {
                applyState(
                    state.copy(
                    currentLineIndex = state.storyLines.size,
                    isStoryCompleted = true,
                    isStoryPlaybackInProgress = false,
                    visualState = FairyTaleVisualState.Waiting(state.visualScheme.completed),
                )
            )

            showAttemptFeedback(
                feedback = FairyTalesFeedbackUi(
                    message = "Ура!",
                    emoji = "🥳",
                ),
                afterDelay = {
                    applyState(state.copy(feedback = null))
                },
            )
        }
    }

    fun onShiftChanged(isEnabled: Boolean) {
        state = state.copy(isShiftEnabled = isEnabled)
        if (studentId.isNotBlank()) {
            runCatching {
                preferencesRepository.setKeyboardShiftEnabled(studentId, isEnabled)
            }
        }
    }

    fun onSymbolPressed(symbol: String) {
        if (state.feedback != null || state.isStoryPlaybackInProgress || state.isStoryCompleted) return
        if (!consumeKeyboardPressThrottle()) return

        if (!preventWrongKeyPress) {
            state = state.copy(answerInput = state.answerInput + symbol)
            return
        }

        val expectedSymbol = expectedSymbolForPressed(
            answerInput = state.answerInput,
            expectedAnswer = state.expectedAnswer,
            pressedSymbol = symbol,
        )
        val acceptsPressedSymbol = acceptsPressedSymbol(
            answerInput = state.answerInput,
            expectedAnswer = state.expectedAnswer,
            pressedSymbol = symbol,
        )

        if (acceptsPressedSymbol && expectedSymbol != null) {
            state = state.copy(answerInput = state.answerInput + symbol)
            triggerTypingFeedback(symbol, TrainingKeyboardFeedbackType.Correct)
            return
        }

        if (expectedSymbol != null) {
            val isStrictSlip = isNeighborKeyboardSlip(
                referenceText = state.expectedAnswer,
                expectedSymbol = expectedSymbol,
                pressedSymbol = symbol,
                sensitivity = NeighborTypoSensitivity.Normal,
            )
            if (isStrictSlip) {
                val shouldTreatAsSlip = allowNeighborTypos && isNeighborKeyboardSlip(
                    referenceText = state.expectedAnswer,
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
        if (state.feedback != null || state.isStoryPlaybackInProgress || state.isStoryCompleted) return
        if (!consumeKeyboardPressThrottle()) return
        state = state.copy(answerInput = state.answerInput.dropLast(1))
    }

    fun onSubmitPressed() {
        if (state.feedback != null || state.isStoryPlaybackInProgress || state.isStoryCompleted) return

        val currentLine = state.currentStoryLine ?: return
        val userInput = state.answerInput.canonicalizeOptionalSpacesForExpected(state.expectedAnswer)
        val isCorrect = userInput.trim().equals(state.expectedAnswer.trim(), ignoreCase = true)
        if (!isCorrect) {
            showAttemptFeedback(
                feedback = FairyTalesFeedbackUi(
                    message = "Попробуй ещё",
                    emoji = "✏️",
                ),
                afterDelay = {
                    state = state.copy(feedback = null)
                },
            )
            return
        }

        markCurrentLineSymbolsAsStudied()

        if (state.lineProgressMode == FairyTaleLineProgressMode.PlaybackThenInput) {
            val nextLineIndex = state.currentLineIndex + 1
            if (nextLineIndex < state.storyLines.size) {
                transitionAfterCurrentAnimationCycle {
                    openStoryLine(nextLineIndex)
                }
            } else {
                completeStory()
            }
            return
        }

        val playbackToken = nextPlaybackToken()
        activePlaybackToken = playbackToken
        state = state.copy(
            isStoryPlaybackInProgress = true,
            keyboardFeedbackKey = null,
            keyboardFeedbackType = null,
            inputFeedbackType = null,
            feedback = null,
        )
        _effects.tryEmit(
            FairyTalesEffect.StartStoryPlayback(
                playbackToken = playbackToken,
                cue = FairyTaleSoundCue(
                    id = "fairy-tale-line-$playbackToken",
                    resourcePath = currentLine.soundResourcePath,
                    fileName = currentLine.soundFileName,
                ),
            ),
        )
    }

    fun onRestartPressed() {
        if (state.feedback != null) return
        resetStoryProgress()
    }

    fun onHintToggle(isEnabled: Boolean) {
        if (state.feedback != null || state.isStoryPlaybackInProgress) return
        state = state.copy(
            usedHint = state.usedHint || isEnabled,
            isInputHintEnabled = isEnabled,
        )
        if (studentId.isNotBlank()) {
            runCatching {
                preferencesRepository.setGalleryInputHintEnabled(studentId, isEnabled)
            }
        }
    }

    fun onShowWordToggle(isEnabled: Boolean) {
        if (state.feedback != null || state.isStoryPlaybackInProgress) return
        state = state.copy(
            usedShowWord = state.usedShowWord || isEnabled,
            isHintVisible = isEnabled,
        )
    }

    fun onSimplifyKeyboardToggle(isEnabled: Boolean) {
        if (state.feedback != null || state.isStoryPlaybackInProgress) return
        state = state.copy(
            usedSimplifiedKeyboard = state.usedSimplifiedKeyboard || isEnabled,
            isSimplifiedKeyboardEnabled = isEnabled,
        )
        if (studentId.isNotBlank()) {
            runCatching {
                preferencesRepository.setFairyTalesSimplifiedKeyboardEnabled(studentId, isEnabled)
            }
        }
        refreshActiveSymbols()
    }

    private fun nextPlaybackToken(): Long {
        nextPlaybackToken += 1L
        return nextPlaybackToken
    }

    private fun expectedSymbolForPressed(
        answerInput: String,
        expectedAnswer: String,
        pressedSymbol: String,
    ): String? {
        val alignment = fairyTalesAnswerInputAlignment(answerInput, expectedAnswer)
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
        val alignment = fairyTalesAnswerInputAlignment(answerInput, expectedAnswer)
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

    private suspend fun loadStudiedSymbols(studentId: String): Set<String> {
        if (studentId.isBlank()) return emptySet()
        return studentRepository.getActiveLettersById(studentId)
            .orEmpty()
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .map { it.toString() }
            .toSet()
    }

    private fun persistStudiedSymbols(symbols: Set<String>) {
        if (studentId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                studentRepository.updateActiveLetters(
                    id = studentId,
                    activeLetters = symbols
                        .mapNotNull { it.singleOrNull() }
                        .joinToString(separator = ""),
                )
            }
        }
    }

    private fun markCurrentLineSymbolsAsStudied() {
        val updatedStudiedSymbols = studiedSymbols + state.expectedAnswer.extractKeyboardSymbols()
        studiedSymbols = updatedStudiedSymbols
        persistStudiedSymbols(updatedStudiedSymbols)
        refreshActiveSymbols()
    }

    private fun refreshActiveSymbols() {
        applyState(
            state.copy(
            activeSymbols = if (state.isSimplifiedKeyboardEnabled) {
                state.expectedAnswer.extractKeyboardSymbols()
            } else {
                studiedSymbols + state.expectedAnswer.extractKeyboardSymbols()
            },
            )
        )
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
        applyState(
            state.copy(
            keyboardFeedbackKey = symbol.lowercase(),
            keyboardFeedbackType = type,
            inputFeedbackType = type,
            )
        )

        keyFeedbackJob = viewModelScope.launch {
            delay(180L)
            applyState(
                state.copy(
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
                )
            )
        }
    }

    private fun showAttemptFeedback(
        feedback: FairyTalesFeedbackUi,
        durationMillis: Long = DefaultFeedbackDurationMillis,
        afterDelay: () -> Unit,
    ) {
        feedbackJob?.cancel()
        applyState(state.copy(feedback = feedback))
        feedbackJob = viewModelScope.launch {
            delay(durationMillis)
            afterDelay()
        }
    }

    private fun openStoryLine(index: Int) {
        val line = state.storyLines.getOrNull(index) ?: return
        applyState(
            state.copy(
            currentLineIndex = index,
            isStoryCompleted = false,
            storyText = line.text,
            expectedAnswer = line.text,
            answerInput = "",
            isStoryPlaybackInProgress = false,
            visualState = FairyTaleVisualState.Waiting(
                waitingVisualFor(
                    visualScheme = state.visualScheme,
                    waitingVisualMode = state.waitingVisualMode,
                    lineIndex = index,
                    linePlaybackVisual = line.playbackVisual,
                )
            ),
            keyboardFeedbackKey = null,
            keyboardFeedbackType = null,
            inputFeedbackType = null,
            isSimplifiedKeyboardEnabled = state.isSimplifiedKeyboardEnabled,
            usedHint = false,
            usedShowWord = false,
            usedSimplifiedKeyboard = false,
            feedback = null,
            )
        )
        refreshActiveSymbols()
        startCurrentLinePlaybackIfNeeded()
    }

    private fun resetStoryProgress() {
        playbackCompletionJob?.cancel()
        animationTransitionJob?.cancel()
        activePlaybackToken = null
        if (state.storyLines.isNotEmpty()) {
            openStoryLine(index = 0)
        } else {
            applyState(
                state.copy(
                answerInput = "",
                isStoryCompleted = false,
                isStoryPlaybackInProgress = false,
                visualState = FairyTaleVisualState.Waiting(state.visualScheme.initial),
                feedback = null,
                )
            )
            refreshActiveSymbols()
        }
    }

    private fun applyState(newState: FairyTalesUiState) {
        val previousRenderKey = state.visualState.renderKey
        state = newState
        if (previousRenderKey != newState.visualState.renderKey) {
            animationCycleStartedAtEpochMillis = nowMillis()
        }
    }

    private fun startCurrentLinePlaybackIfNeeded() {
        if (state.lineProgressMode != FairyTaleLineProgressMode.PlaybackThenInput) return
        val currentLine = state.currentStoryLine ?: return
        val playbackToken = nextPlaybackToken()
        activePlaybackToken = playbackToken
        applyState(
            state.copy(
                isStoryPlaybackInProgress = true,
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
                feedback = null,
            )
        )
        _effects.tryEmit(
            FairyTalesEffect.StartStoryPlayback(
                playbackToken = playbackToken,
                cue = FairyTaleSoundCue(
                    id = "fairy-tale-line-$playbackToken",
                    resourcePath = currentLine.soundResourcePath,
                    fileName = currentLine.soundFileName,
                ),
            ),
        )
    }

    private fun completeStory() {
        applyState(
            state.copy(
                currentLineIndex = state.storyLines.size,
                isStoryCompleted = true,
                isStoryPlaybackInProgress = false,
                visualState = FairyTaleVisualState.Waiting(state.visualScheme.completed),
            )
        )

        _effects.tryEmit(
            FairyTalesEffect.PlayOneShotSound(
                cue = FairyTaleSoundCue(
                    id = "fairy-tale-complete-fanfare",
                    resourcePath = FairyTaleCompletionFanfarePath,
                    fileName = FairyTaleCompletionFanfareFileName,
                ),
            )
        )

        showAttemptFeedback(
            feedback = FairyTalesFeedbackUi(
                message = "Ура!",
                emoji = "🥳",
            ),
            durationMillis = CompletionFeedbackDurationMillis,
            afterDelay = {
                applyState(state.copy(feedback = null))
            },
        )
    }

    private fun transitionAfterCurrentAnimationCycle(
        onTransition: () -> Unit,
    ) {
        animationTransitionJob?.cancel()
        val loopDurationMillis = state.visualState.content.loopDurationMillis ?: 0L
        if (loopDurationMillis <= 0L) {
            onTransition()
            return
        }

        val elapsedMillis = (nowMillis() - animationCycleStartedAtEpochMillis).coerceAtLeast(0L)
        val remainderMillis = (loopDurationMillis - (elapsedMillis % loopDurationMillis))
            .let { if (it == loopDurationMillis) 0L else it }

        if (remainderMillis <= 0L) {
            onTransition()
            return
        }

        animationTransitionJob = viewModelScope.launch {
            delay(remainderMillis)
            onTransition()
        }
    }

    override fun onCleared() {
        animationTransitionJob?.cancel()
        playbackCompletionJob?.cancel()
        keyFeedbackJob?.cancel()
        feedbackJob?.cancel()
        super.onCleared()
    }
}

private fun FairyTaleContent.toUiState(): FairyTalesUiState {
    val initialLine = storyLines.firstOrNull()
    return FairyTalesUiState(
        fairyTaleId = id,
        title = title,
        description = description,
        coverColor = coverColor,
        coverRes = coverRes,
        visualScheme = visualScheme,
        waitingVisualMode = waitingVisualMode,
        lineProgressMode = lineProgressMode,
        visualState = FairyTaleVisualState.Waiting(
            waitingVisualFor(
                visualScheme = visualScheme,
                waitingVisualMode = waitingVisualMode,
                lineIndex = 0,
                linePlaybackVisual = initialLine?.playbackVisual ?: FairyTaleVisualContent.None,
            )
        ),
        storyLines = storyLines,
        currentLineIndex = 0,
        storyText = initialLine?.text ?: title,
        expectedAnswer = initialLine?.text ?: title,
    )
}

private fun waitingVisualFor(
    visualScheme: FairyTaleVisualScheme,
    waitingVisualMode: FairyTaleWaitingVisualMode,
    lineIndex: Int,
    linePlaybackVisual: FairyTaleVisualContent,
): FairyTaleVisualContent = when (waitingVisualMode) {
    FairyTaleWaitingVisualMode.CurrentLinePlaybackVisual -> linePlaybackVisual
    FairyTaleWaitingVisualMode.VisualScheme -> if (lineIndex == 0) {
        visualScheme.initial
    } else {
        visualScheme.betweenLines
    }
}
