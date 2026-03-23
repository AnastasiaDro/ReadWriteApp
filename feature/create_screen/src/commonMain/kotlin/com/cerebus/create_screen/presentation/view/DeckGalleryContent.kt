package com.cerebus.create_screen.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.FeedbackOverlay
import com.cerebus.core.ui.components.GameLikeActiveScreenShell
import com.cerebus.core.ui.components.GameLikeScreenShell
import com.cerebus.core.ui.components.PracticeModeStatusIcon
import com.cerebus.create_screen.presentation.DeckGalleryStrings
import com.cerebus.create_screen.presentation.DeckGalleryUiState
import com.cerebus.create_screen.presentation.GalleryGameLikeAnswerSection
import com.cerebus.create_screen.presentation.GalleryTopRightHelpChips
import com.cerebus.create_screen.presentation.resolveGalleryAnswerSectionMinHeight
import com.cerebus.customkeyboard.TrainingKeyboard
import com.cerebus.customkeyboard.resolveShowDigitsRow
import com.cerebus.customkeyboard.resolveTrainingKeyboardHeight

@Composable
internal fun DeckGalleryScreen(
    state: DeckGalleryUiState,
    strings: DeckGalleryStrings,
    animatedScrollTargetIndex: Int?,
    onAnimatedScrollTargetConsumed: () -> Unit,
    onBackClick: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShiftChanged: (Boolean) -> Unit,
    onSymbolPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onSubmitPressed: () -> Unit,
    onHintToggle: (Boolean) -> Unit,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
) {
    var showPracticeModeInfo by remember { mutableStateOf(false) }

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
    val allowMultilineAnswer = !isPhoneLandscape &&
        (currentCard?.name?.let { it.length > 10 || it.contains(' ') } == true)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) {
                    Text("✕")
                }

                if (currentCard != null) {
                    PracticeModeStatusIcon(
                        onClick = { showPracticeModeInfo = true },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        topCenter = {
            if (currentCard != null && !isLandscape) {
                TextButton(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.textButtonColors(
                        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(
                        text = "${state.currentIndex + 1} / ${cards.size}",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        topRight = {
            if (currentCard != null) {
                GalleryTopRightHelpChips(
                    isHintEnabled = state.isInputHintEnabled,
                    isShowWordEnabled = state.isHintVisible,
                    isSimplifiedKeyboardEnabled = state.isSimplifiedKeyboardEnabled,
                    usedHint = state.usedHint,
                    usedShowWord = state.usedShowWord,
                    usedSimplifiedKeyboard = state.usedSimplifiedKeyboard,
                    onHintToggle = onHintToggle,
                    onShowWordToggle = onShowWordToggle,
                    onSimplifyKeyboardToggle = onSimplifyKeyboardToggle,
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GameLikeActiveScreenShell(
                showKeyboard = isKeyboardVisible && currentCard != null,
                isLandscape = isLandscape,
                keyboardTopPadding = if (isPhoneLandscape) 4.dp else if (isLandscape) 8.dp else 4.dp,
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
                        onSettingsPressed = onOpenKeyboardSettings,
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
                    val answerTextScale = when {
                        isPhoneLandscape -> 1f
                        isTablet -> 2f
                        else -> 1.5f
                    }
                    val inputSectionHeight = resolveGalleryAnswerSectionMinHeight(
                        baseLineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        textScale = answerTextScale,
                        allowMultiline = allowMultilineAnswer,
                        density = density,
                        minimumHeight = if (isPhoneLandscape) 48.dp else 56.dp,
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
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Top,
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    val landscapeGap = if (isPhoneLandscape) 6.dp else 10.dp
                                    val landscapeCardSize = minOf(
                                        maxHeight - inputSectionHeight - landscapeGap,
                                        if (isPhoneLandscape) maxWidth - 16.dp else 360.dp,
                                    ).coerceAtLeast(120.dp)
                                    val landscapeAnswerMaxWidth = maxWidth - 24.dp

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                    ) {
                                        GalleryTrainingCard(
                                            cards = cards,
                                            currentIndex = state.currentIndex,
                                            animatedScrollTargetIndex = animatedScrollTargetIndex,
                                            onAnimatedScrollTargetConsumed = onAnimatedScrollTargetConsumed,
                                            isHintVisible = state.isHintVisible,
                                            isLandscape = true,
                                            isPhoneLandscape = isPhoneLandscape,
                                            preferredCardSize = landscapeCardSize,
                                            modifier = Modifier.fillMaxWidth(),
                                            onCardSelected = view@{ index ->
                                                when {
                                                    index < state.currentIndex -> onPreviousClick()
                                                    index > state.currentIndex -> onNextClick()
                                                }
                                            },
                                        )
                                        Spacer(modifier = Modifier.height(landscapeGap))
                                        GalleryGameLikeAnswerSection(
                                            answerInput = state.answerInput,
                                            expectedAnswer = currentCard.name,
                                            inputFeedbackType = state.inputFeedbackType,
                                            isHintEnabled = state.isInputHintEnabled,
                                            isShiftEnabled = state.isShiftEnabled,
                                            availableWidth = landscapeAnswerMaxWidth,
                                            fieldReferenceWidth = landscapeAnswerMaxWidth,
                                            isStacked = false,
                                            isCompact = isPhoneLandscape,
                                            isAdaptiveWidth = true,
                                            minimumFieldWidth = if (isPhoneLandscape) 96.dp else 220.dp,
                                            modifier = Modifier.widthIn(max = landscapeAnswerMaxWidth),
                                            onFieldClick = { isKeyboardVisible = true },
                                            onSubmit = onSubmitPressed,
                                        )
                                    }
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
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (isTablet) 40.dp else 8.dp,
                                            top = if (isTablet) 20.dp else 16.dp,
                                            end = if (isTablet) 40.dp else 8.dp,
                                            bottom = if (isTablet) 12.dp else 0.dp,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val portraitContentWidth = minOf(
                                        maxWidth,
                                        maxHeight - inputSectionHeight - verticalSpacing,
                                    ).coerceAtLeast(120.dp)

                                    Column(
                                        modifier = Modifier.width(portraitContentWidth),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                                    ) {
                                        GalleryTrainingCard(
                                            cards = cards,
                                            currentIndex = state.currentIndex,
                                            animatedScrollTargetIndex = animatedScrollTargetIndex,
                                            onAnimatedScrollTargetConsumed = onAnimatedScrollTargetConsumed,
                                            isHintVisible = state.isHintVisible,
                                            isLandscape = false,
                                            isPhoneLandscape = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            onCardSelected = view@{ index ->
                                                when {
                                                    index < state.currentIndex -> onPreviousClick()
                                                    index > state.currentIndex -> onNextClick()
                                                }
                                            },
                                        )

                                        GalleryGameLikeAnswerSection(
                                            answerInput = state.answerInput,
                                            expectedAnswer = currentCard.name,
                                            inputFeedbackType = state.inputFeedbackType,
                                            isHintEnabled = state.isInputHintEnabled,
                                            isShiftEnabled = state.isShiftEnabled,
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

                        FeedbackOverlay(
                            message = state.feedback?.message,
                            emoji = state.feedback?.emoji,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }

    if (showPracticeModeInfo) {
        AlertDialog(
            onDismissRequest = { showPracticeModeInfo = false },
            title = {
                Text(strings.practiceModeTitle)
            },
            text = {
                Text(strings.practiceModeDescription)
            },
            confirmButton = {
                TextButton(onClick = { showPracticeModeInfo = false }) {
                    Text(strings.practiceModeUnderstood)
                }
            },
        )
    }
}
