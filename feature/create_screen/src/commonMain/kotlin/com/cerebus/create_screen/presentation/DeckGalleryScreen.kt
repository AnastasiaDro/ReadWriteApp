package com.cerebus.create_screen.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.cerebus.core.ui.components.AnswerFieldVerticalPadding
import com.cerebus.core.ui.components.GameLikeActiveScreenShell
import com.cerebus.core.ui.components.AnswerInputRow
import com.cerebus.core.ui.components.GameLikeScreenShell
import com.cerebus.core.ui.components.OverlayHelpToggleChip
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.utils.nowMillis
import com.cerebus.customkeyboard.TrainingKeyboard
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.customkeyboard.isNeighborKeyboardSlip
import com.cerebus.customkeyboard.resolveTrainingKeyboardHeight
import com.cerebus.customkeyboard.resolveShowDigitsRow
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val GALLERY_FEEDBACK_DURATION_MS = 1000L

data class DeckGalleryStrings(
    val back: String,
    val titleFallback: String,
    val previous: String,
    val next: String,
    val empty: String,
    val edit: String,
    val submit: String,
    val showWord: String,
    val simplifyKeyboard: String,
    val practiceMode: String,
    val correctFeedback: String,
    val wrongFeedback: String,
)

enum class DeckGalleryLearningStage {
    Copy,
    Recall,
}

data class DeckGalleryUiState(
    val isLoading: Boolean = true,
    val deckId: String = "",
    val deckName: String = "",
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val activeSymbols: Set<String> = emptySet(),
    val isShiftEnabled: Boolean = false,
    val learningStage: DeckGalleryLearningStage = DeckGalleryLearningStage.Copy,
    val keyboardFeedbackKey: String? = null,
    val keyboardFeedbackType: TrainingKeyboardFeedbackType? = null,
    val inputFeedbackType: TrainingKeyboardFeedbackType? = null,
    val answerInput: String = "",
    val isHintVisible: Boolean = true,
    val isSimplifiedKeyboardEnabled: Boolean = false,
    val hideDigitsOnTightScreen: Boolean = true,
    val copySuccessStreak: Int = 0,
    val wrongPressCount: Int = 0,
    val slipPressCount: Int = 0,
    val usedShowWord: Boolean = false,
    val usedSimplifiedKeyboard: Boolean = false,
    val feedback: DeckGalleryFeedbackUi? = null,
) {
    val currentCard: Flashcard?
        get() = cards.getOrNull(currentIndex)
}

data class DeckGalleryFeedbackUi(
    val message: String,
    val emoji: String,
)

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
    private var freeNeighborSlipPresses: Int = 1
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
            freeNeighborSlipPresses = (preferencesRepository.getFreeNeighborSlipPresses(studentId) ?: 1)
                .coerceIn(0, 3)
            keyboardPressDelayMs = (preferencesRepository.getKeyboardPressDelay(studentId)
                ?: KeyboardPressDelay.Normal).intervalMs
            _uiState.update {
                it.copy(
                    isLoading = true,
                    deckId = deckId,
                    isShiftEnabled = preferencesRepository.getKeyboardShiftEnabled(studentId) == true,
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
                        deckName = deck?.name.orEmpty(),
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
                _uiState.update {
                    if (shouldTreatAsSlip) {
                        it.copy(slipPressCount = it.slipPressCount + 1)
                    } else {
                        it.copy(wrongPressCount = it.wrongPressCount + 1)
                    }
                }
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

        _uiState.update { it.copy(wrongPressCount = it.wrongPressCount + 1) }
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
                resetAttemptForCurrentCard(
                    keepLearningStage = DeckGalleryLearningStage.Copy,
                    keepHintVisible = current.isHintVisible,
                    keepCopySuccessStreak = 0,
                )
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
                learningStage = DeckGalleryLearningStage.Copy,
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
                answerInput = "",
                isHintVisible = keepHintVisible,
                isSimplifiedKeyboardEnabled = false,
                copySuccessStreak = 0,
                wrongPressCount = 0,
                slipPressCount = 0,
                usedShowWord = false,
                usedSimplifiedKeyboard = false,
                feedback = null,
            )
        }
        refreshActiveSymbols()
    }

    private fun resetAttemptForCurrentCard(
        keepLearningStage: DeckGalleryLearningStage,
        keepHintVisible: Boolean,
        keepCopySuccessStreak: Int,
    ) {
        lastHandledKeyPressAtEpochMillis = 0L
        keyFeedbackJob?.cancel()
        _uiState.update { state ->
            state.copy(
                learningStage = keepLearningStage,
                answerInput = "",
                isHintVisible = keepHintVisible,
                isSimplifiedKeyboardEnabled = false,
                keyboardFeedbackKey = null,
                keyboardFeedbackType = null,
                inputFeedbackType = null,
                copySuccessStreak = keepCopySuccessStreak,
                wrongPressCount = 0,
                slipPressCount = 0,
                usedShowWord = false,
                usedSimplifiedKeyboard = false,
                feedback = null,
            )
        }
        refreshActiveSymbols()
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
}

@Composable
fun DeckGalleryRoute(
    deckId: String,
    initialCardId: String?,
    strings: DeckGalleryStrings,
    onBackClick: () -> Unit,
    onEditCard: (String, String) -> Unit,
) {
    val viewModel = koinViewModel<DeckGalleryViewModel>(
        parameters = { parametersOf(deckId, initialCardId) },
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(deckId, initialCardId) {
        viewModel.load()
    }

    DeckGalleryScreen(
        state = state,
        strings = strings,
        onBackClick = onBackClick,
        onPreviousClick = viewModel::showPrevious,
        onNextClick = viewModel::showNext,
        onCardSelected = viewModel::showCard,
        onShiftChanged = viewModel::onShiftChanged,
        onSymbolPressed = viewModel::onSymbolPressed,
        onBackspacePressed = viewModel::onBackspacePressed,
        onSubmitPressed = { viewModel.onSubmitPressed(strings) },
        onShowWordToggle = viewModel::onShowWordToggle,
        onSimplifyKeyboardToggle = viewModel::onSimplifyKeyboardToggle,
    )
}

@Composable
private fun DeckGalleryScreen(
    state: DeckGalleryUiState,
    strings: DeckGalleryStrings,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCardSelected: (Int) -> Unit,
    onShiftChanged: (Boolean) -> Unit,
    onSymbolPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onSubmitPressed: () -> Unit,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val cards = state.cards
    val currentCard = state.currentCard
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val isLandscape = windowWidthDp > windowHeightDp
    val isTablet = minOf(windowWidthDp, windowHeightDp) >= 600.dp
    val isPhoneLandscape = isLandscape && !isTablet
    val showDigitsRow = resolveShowDigitsRow(
        isPhoneLandscape = isPhoneLandscape,
        hideDigitsOnTightScreen = state.hideDigitsOnTightScreen,
        referenceText = currentCard?.name.orEmpty(),
    )
    var isKeyboardVisible by remember { mutableStateOf(true) }
    val showShowWordToggle = true
    val showSimplifyToggle = true
    val allowMultilineAnswer = currentCard?.name?.let { it.length > 10 || it.contains(' ') } == true
    val keyboardHeight = remember(windowWidthDp, windowHeightDp, showDigitsRow) {
        val baseHeight = if (isLandscape) {
            (windowHeightDp * 0.5f).coerceIn(220.dp, 340.dp)
        } else {
            (windowHeightDp * 0.3f).coerceIn(220.dp, 340.dp)
        }
        resolveTrainingKeyboardHeight(
            baseHeight = baseHeight,
            showDigitsRow = showDigitsRow,
        )
    }

    GameLikeScreenShell(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topLeft = {
            TextButton(onClick = onBackClick) {
                Text("✕")
            }
        },
        topRight = {
            if (currentCard != null) {
                GalleryTopRightHelpChips(
                    showWordChip = showShowWordToggle,
                    isShowWordEnabled = state.isHintVisible,
                    isSimplifiedKeyboardEnabled = state.isSimplifiedKeyboardEnabled,
                    usedShowWord = state.usedShowWord,
                    usedSimplifiedKeyboard = state.usedSimplifiedKeyboard,
                    onShowWordToggle = onShowWordToggle,
                    onSimplifyKeyboardToggle = onSimplifyKeyboardToggle,
                )
            }
        },
    ) {
        GameLikeActiveScreenShell(
            showKeyboard = isKeyboardVisible && currentCard != null,
            isLandscape = isLandscape,
            keyboard = {
                TrainingKeyboard(
                    referenceText = currentCard?.name.orEmpty(),
                    activeSymbols = state.activeSymbols,
                    isShiftEnabled = state.isShiftEnabled,
                    feedbackKey = state.keyboardFeedbackKey,
                    feedbackType = state.keyboardFeedbackType,
                    onShiftChanged = onShiftChanged,
                    onSymbolPressed = onSymbolPressed,
                    onBackspacePressed = onBackspacePressed,
                    onSpacePressed = { onSymbolPressed(" ") },
                    onSubmitPressed = onSubmitPressed,
                    onSettingsPressed = {},
                    showDigitsRow = showDigitsRow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = keyboardHeight, max = keyboardHeight),
                )
            },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(
                        top = when {
                            isPhoneLandscape -> 8.dp
                            isLandscape -> 20.dp
                            else -> 24.dp
                        },
                        bottom = if (isLandscape) 0.dp else 12.dp,
                    ),
            ) {
                val availableContentWidth = maxWidth
                val availableContentHeight = maxHeight
                val answerTextScale = if (isTablet) 2f else 1.5f
                val inputSectionHeight = resolveGalleryAnswerSectionMinHeight(
                    baseLineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    textScale = answerTextScale,
                    allowMultiline = allowMultilineAnswer,
                    density = density,
                )
                val verticalSpacing = if (isLandscape) 12.dp else 16.dp

                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentCard == null) {
                        Text(
                            text = strings.empty,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else if (isLandscape && isKeyboardVisible) {
                        val landscapeCardSize = minOf(
                            availableContentHeight - 8.dp,
                            availableContentWidth,
                        ).coerceAtLeast(120.dp)
                        val landscapeHalfWidth = ((availableContentWidth - 8.dp) / 2f).coerceAtLeast(140.dp)
                        val counterWidth = 28.dp

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Top,
                        ) {
                            if (isPhoneLandscape) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomEnd,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.Bottom,
                                    ) {
                                        GalleryPracticeModeBadge(
                                            text = strings.practiceMode,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                        ) {
                                            Text(
                                                text = "${state.currentIndex + 1}/${cards.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.widthIn(min = counterWidth),
                                                textAlign = TextAlign.Center,
                                            )
                                            Spacer(modifier = Modifier.widthIn(min = 6.dp, max = 6.dp))
                                            GalleryTrainingCard(
                                                card = currentCard,
                                                isHintVisible = state.isHintVisible,
                                                modifier = Modifier.widthIn(max = landscapeCardSize),
                                                onSwipePrevious = if (state.currentIndex > 0) onPreviousClick else null,
                                                onSwipeNext = if (state.currentIndex < cards.lastIndex) onNextClick else null,
                                                previousLabel = strings.previous,
                                                nextLabel = strings.next,
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.widthIn(min = 8.dp, max = 8.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomStart,
                                ) {
                                    GalleryGameLikeAnswerSection(
                                        answerInput = state.answerInput,
                                        expectedAnswer = currentCard.name,
                                        inputFeedbackType = state.inputFeedbackType,
                                        availableWidth = minOf(landscapeHalfWidth, 280.dp),
                                        fieldReferenceWidth = landscapeCardSize,
                                        isStacked = false,
                                        alignToStart = true,
                                        onFieldClick = { isKeyboardVisible = true },
                                        onSubmit = onSubmitPressed,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        text = "${state.currentIndex + 1}/${cards.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(end = 12.dp),
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.BottomCenter,
                                    ) {
                                        val tabletContentWidth = minOf(
                                            maxWidth,
                                            maxHeight - inputSectionHeight - 10.dp,
                                        ).coerceAtLeast(120.dp)

                                        Column(
                                            modifier = Modifier
                                                .widthIn(max = tabletContentWidth)
                                                .fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                        ) {
                                            GalleryPracticeModeBadge(
                                                text = strings.practiceMode,
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            GalleryTrainingCard(
                                                card = currentCard,
                                                isHintVisible = state.isHintVisible,
                                                modifier = Modifier.fillMaxWidth(),
                                                onSwipePrevious = if (state.currentIndex > 0) onPreviousClick else null,
                                                onSwipeNext = if (state.currentIndex < cards.lastIndex) onNextClick else null,
                                                previousLabel = strings.previous,
                                                nextLabel = strings.next,
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            GalleryGameLikeAnswerSection(
                                                answerInput = state.answerInput,
                                                expectedAnswer = currentCard.name,
                                                inputFeedbackType = state.inputFeedbackType,
                                                availableWidth = tabletContentWidth,
                                                fieldReferenceWidth = tabletContentWidth,
                                                isStacked = false,
                                                onFieldClick = { isKeyboardVisible = true },
                                                onSubmit = onSubmitPressed,
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = if (isTablet) 30.dp else 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                        ) {
                            GalleryPracticeModeBadge(
                                text = strings.practiceMode,
                            )
                            Text(
                                text = "${state.currentIndex + 1} / ${cards.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (isTablet) 40.dp else 16.dp,
                                        top = if (isTablet) 20.dp else 16.dp,
                                        end = if (isTablet) 40.dp else 16.dp,
                                        bottom = if (isTablet) 12.dp else 0.dp,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                val portraitContentWidth = minOf(
                                    maxWidth,
                                    maxHeight - inputSectionHeight - verticalSpacing,
                                ).coerceAtLeast(120.dp)

                                Column(
                                    modifier = Modifier.widthIn(max = portraitContentWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                                ) {
                                    GalleryTrainingCard(
                                        card = currentCard,
                                        isHintVisible = state.isHintVisible,
                                        modifier = Modifier.fillMaxWidth(),
                                        onSwipePrevious = if (state.currentIndex > 0) onPreviousClick else null,
                                        onSwipeNext = if (state.currentIndex < cards.lastIndex) onNextClick else null,
                                        previousLabel = strings.previous,
                                        nextLabel = strings.next,
                                    )

                                    GalleryGameLikeAnswerSection(
                                        answerInput = state.answerInput,
                                        expectedAnswer = currentCard.name,
                                        inputFeedbackType = state.inputFeedbackType,
                                        availableWidth = portraitContentWidth,
                                        fieldReferenceWidth = portraitContentWidth,
                                        isStacked = true,
                                        onFieldClick = { isKeyboardVisible = true },
                                        onSubmit = onSubmitPressed,
                                    )
                                }
                            }
                        }
                    }

                    GalleryFeedbackBanner(
                        feedback = state.feedback,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryGameLikeAnswerSection(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    availableWidth: androidx.compose.ui.unit.Dp,
    fieldReferenceWidth: androidx.compose.ui.unit.Dp,
    isStacked: Boolean,
    alignToStart: Boolean = false,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val checkButtonWidth = 56.dp
    val buttonSpacing = 12.dp
    val maxFieldWidth = (availableWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(140.dp)
    val fieldWidth = if (isStacked) maxFieldWidth else minOf(fieldReferenceWidth, maxFieldWidth)
    val allowMultilineAnswer = expectedAnswer.length > 10 || expectedAnswer.contains(' ')

    Column(
        modifier = if (alignToStart) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignToStart) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnswerInputRow(
            answerInput = answerInput,
            expectedAnswer = expectedAnswer,
            feedbackBorderColor = when (inputFeedbackType) {
                TrainingKeyboardFeedbackType.Correct -> Color(0xFF9AD88F)
                TrainingKeyboardFeedbackType.Slip -> Color(0xFFFFD35C)
                TrainingKeyboardFeedbackType.Wrong -> Color(0xFFFF7A7A)
                null -> Color.Unspecified
            },
            allowMultilineAnswer = allowMultilineAnswer,
            fieldWidth = fieldWidth,
            alignToStart = alignToStart,
            onFieldClick = onFieldClick,
            onSubmit = onSubmit,
        )
    }
}

private fun resolveGalleryAnswerSectionMinHeight(
    baseLineHeight: TextUnit,
    textScale: Float,
    allowMultiline: Boolean,
    density: Density,
): androidx.compose.ui.unit.Dp {
    val lineCount = if (allowMultiline) 2 else 1
    val scaledLineHeight = with(density) { (baseLineHeight * textScale).toDp() }
    val fieldHeight = scaledLineHeight * lineCount + (AnswerFieldVerticalPadding * 2)
    val answerRowHeight = maxOf(fieldHeight, 56.dp)
    return answerRowHeight
}

@Composable
private fun GalleryTopRightHelpChips(
    showWordChip: Boolean,
    isShowWordEnabled: Boolean,
    isSimplifiedKeyboardEnabled: Boolean,
    usedShowWord: Boolean,
    usedSimplifiedKeyboard: Boolean,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (showWordChip) {
            OverlayHelpToggleChip(
                label = "Word",
                checked = isShowWordEnabled,
                wasUsed = usedShowWord,
                onClick = { onShowWordToggle(!isShowWordEnabled) },
            )
        }
        OverlayHelpToggleChip(
            label = "Aa",
            checked = isSimplifiedKeyboardEnabled,
            wasUsed = usedSimplifiedKeyboard,
            onClick = { onSimplifyKeyboardToggle(!isSimplifiedKeyboardEnabled) },
        )
    }
}

@Composable
private fun GallerySectionCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun GalleryTrainingCard(
    card: Flashcard,
    isHintVisible: Boolean,
    modifier: Modifier = Modifier,
    onSwipePrevious: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null,
    previousLabel: String,
    nextLabel: String,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext).build()
    }
    var imageLoadFailed by remember(card.imageUrl) { mutableStateOf(false) }
    val normalizedImagePath = card.imageUrl.trim()
    val isTextCard = normalizedImagePath.isBlank() || imageLoadFailed

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val cardSize = if (maxWidth < 360.dp) maxWidth else 360.dp
        var dragAccumulation by remember(card.id) { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .align(Alignment.Center)
                .pointerInput(card.id, onSwipePrevious, onSwipeNext) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulation += dragAmount
                        },
                        onDragEnd = {
                            when {
                                dragAccumulation <= -56f -> onSwipeNext?.invoke()
                                dragAccumulation >= 56f -> onSwipePrevious?.invoke()
                            }
                            dragAccumulation = 0f
                        },
                        onDragCancel = {
                            dragAccumulation = 0f
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .size(cardSize)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (isTextCard) {
                    GalleryCardTextFallback(
                        text = card.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = normalizedImagePath,
                        contentDescription = null,
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { imageLoadFailed = false },
                        onError = { imageLoadFailed = true },
                    )
                }

                GalleryHint(
                    text = card.name.uppercase(),
                    visible = isHintVisible && !isTextCard,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }
        }

        GallerySideArrowButton(
            symbol = "‹",
            label = previousLabel,
            enabled = onSwipePrevious != null,
            onClick = { onSwipePrevious?.invoke() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
        )

        GallerySideArrowButton(
            symbol = "›",
            label = nextLabel,
            enabled = onSwipeNext != null,
            onClick = { onSwipeNext?.invoke() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
        )
    }
}

@Composable
private fun GallerySideArrowButton(
    symbol: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "gallery_side_arrow_scale",
    )
    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(999.dp),
        color = if (!enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else if (isPressed) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (!enabled) {
            0.dp
        } else if (isPressed) {
            4.dp
        } else {
            2.dp
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (!enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                } else if (isPressed) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}


@Composable
private fun GalleryAnswerInputSection(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    showShowWordToggle: Boolean,
    showSimplifyToggle: Boolean,
    isShowWordEnabled: Boolean,
    isSimplifiedKeyboardEnabled: Boolean,
    usedShowWord: Boolean,
    usedSimplifiedKeyboard: Boolean,
    submitText: String,
    showWordText: String,
    simplifyKeyboardText: String,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
) {
    val allowMultilineAnswer = expectedAnswer.length > 10 || expectedAnswer.contains(' ')
    val answerFieldHeight = if (allowMultilineAnswer) 76.dp else 52.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GalleryReadOnlyAnswerField(
            value = answerInput,
            expectedAnswer = expectedAnswer,
            inputFeedbackType = inputFeedbackType,
            allowMultiline = allowMultilineAnswer,
            modifier = Modifier
                .fillMaxWidth()
                .height(answerFieldHeight),
            onClick = onFieldClick,
        )

        OutlinedButton(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(submitText)
        }

        if (showShowWordToggle || showSimplifyToggle) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showShowWordToggle) {
                    GalleryHelpToggleRow(
                        label = showWordText,
                        checked = isShowWordEnabled,
                        wasUsed = usedShowWord,
                        onCheckedChange = onShowWordToggle,
                    )
                }
                if (showSimplifyToggle) {
                    GalleryHelpToggleRow(
                        label = simplifyKeyboardText,
                        checked = isSimplifiedKeyboardEnabled,
                        wasUsed = usedSimplifiedKeyboard,
                        onCheckedChange = onSimplifyKeyboardToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryHelpToggleRow(
    label: String,
    checked: Boolean,
    wasUsed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (wasUsed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun GalleryReadOnlyAnswerField(
    value: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    allowMultiline: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val currentSlotBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
    val currentSlotTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    val displayedValue = remember(
        value,
        expectedAnswer,
        currentSlotBackgroundColor,
        currentSlotTextColor,
    ) {
        buildGalleryAnswerProgressMask(
            answerInput = value,
            expectedAnswer = expectedAnswer,
            currentSlotBackgroundColor = currentSlotBackgroundColor,
            currentSlotTextColor = currentSlotTextColor,
        )
    }
    val feedbackBorderColor = when (inputFeedbackType) {
        TrainingKeyboardFeedbackType.Correct -> Color(0xFF9AD88F)
        TrainingKeyboardFeedbackType.Slip -> Color(0xFFFFD35C)
        TrainingKeyboardFeedbackType.Wrong -> Color(0xFFFF7A7A)
        null -> MaterialTheme.colorScheme.outline
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = !allowMultiline,
            minLines = 1,
            maxLines = if (allowMultiline) 2 else 1,
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = feedbackBorderColor,
                unfocusedBorderColor = feedbackBorderColor,
            ),
        )
        Text(
            text = displayedValue,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (allowMultiline) 2 else 1,
            modifier = Modifier
                .align(if (allowMultiline) Alignment.TopStart else Alignment.CenterStart)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        )
    }
}

private fun buildGalleryAnswerProgressMask(
    answerInput: String,
    expectedAnswer: String,
    currentSlotBackgroundColor: Color,
    currentSlotTextColor: Color,
): AnnotatedString {
    if (expectedAnswer.isEmpty()) return AnnotatedString(answerInput)
    return buildAnnotatedString {
        val currentSlotIndex = nextGalleryVisibleSlotIndex(
            answerInput = answerInput,
            expectedAnswer = expectedAnswer,
        )
        var inputIndex = 0
        expectedAnswer.forEachIndexed { index, expectedChar ->
            if (index > 0) append(' ')
            val displayedChar = when {
                expectedChar.isWhitespace() && answerInput.getOrNull(inputIndex)?.isWhitespace() == true -> {
                    inputIndex++
                    ' '
                }
                expectedChar.isWhitespace() -> ' '
                inputIndex < answerInput.length -> answerInput[inputIndex++]
                else -> '_'
            }
            val isCurrentSlot = index == currentSlotIndex

            if (isCurrentSlot) {
                pushStyle(
                    SpanStyle(
                        background = currentSlotBackgroundColor,
                        color = currentSlotTextColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                )
                append(displayedChar)
                pop()
            } else {
                append(displayedChar)
            }
        }
    }
}

private fun nextGalleryVisibleSlotIndex(
    answerInput: String,
    expectedAnswer: String,
): Int {
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
            else -> {
                expectedIndex++
                inputIndex++
            }
        }
    }

    while (expectedIndex < expectedAnswer.length && expectedAnswer[expectedIndex].isWhitespace()) {
        expectedIndex++
    }

    return expectedIndex.takeIf { it in expectedAnswer.indices } ?: -1
}

@Composable
private fun GalleryCardTextFallback(
    text: String,
    modifier: Modifier = Modifier,
) {
    val normalizedText = text.trim().ifBlank { "?" }
    val textStyle = when {
        normalizedText.length <= 2 -> MaterialTheme.typography.displayLarge
        normalizedText.length <= 6 -> MaterialTheme.typography.displayMedium
        normalizedText.length <= 12 -> MaterialTheme.typography.displaySmall
        else -> MaterialTheme.typography.headlineLarge
    }

    Text(
        text = normalizedText,
        style = textStyle,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun GalleryFeedbackBanner(
    feedback: DeckGalleryFeedbackUi?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(if (feedback == null) 0.dp else 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = feedback?.emoji.orEmpty(),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = feedback?.message.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GalleryPracticeModeBadge(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun GalleryHint(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GalleryCardThumbnail(
    card: Flashcard,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext).build()
    }
    val imagePath = card.imageUrl.trim()

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath.isBlank()) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun Flashcard.extractKeyboardSymbols(): Set<String> {
    return name
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .map { it.toString() }
        .toSet()
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

private data class GalleryAnswerInputAlignment(
    val nextExpectedIndex: Int,
)

private fun answerInputAlignment(
    answerInput: String,
    expectedAnswer: String,
): GalleryAnswerInputAlignment {
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

    while (expectedIndex < expectedAnswer.length && expectedAnswer[expectedIndex].isWhitespace()) {
        val nextInputChar = answerInput.getOrNull(inputIndex)
        if (nextInputChar?.isWhitespace() == true) break
        expectedIndex++
    }

    return GalleryAnswerInputAlignment(nextExpectedIndex = expectedIndex)
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
