package com.cerebus.create_screen.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
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
import com.cerebus.customkeyboard.TrainingKeyboard
import com.cerebus.customkeyboard.resolveShowDigitsRow
import com.cerebus.customkeyboard.resolveTrainingKeyboardHeight

@Composable
internal fun DeckGalleryScreen(
    state: DeckGalleryUiState,
    strings: DeckGalleryStrings,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShiftChanged: (Boolean) -> Unit,
    onSymbolPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onSubmitPressed: () -> Unit,
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
    val navigationControlShift = if (isPhoneLandscape) 58.dp else 0.dp
    val navigationControlsHorizontalPadding = if (isTablet) 6.dp else 0.dp
    val showDigitsRow = resolveShowDigitsRow(
        isPhoneLandscape = isPhoneLandscape,
        hideDigitsOnTightScreen = state.hideDigitsOnTightScreen,
        referenceText = currentCard?.name.orEmpty(),
    )
    var isKeyboardVisible by remember { mutableStateOf(true) }
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
        topRight = {
            if (currentCard != null) {
                GalleryTopRightHelpChips(
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.BottomEnd,
                                    ) {
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
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.widthIn(min = 8.dp, max = 8.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize(),
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
                                                modifier = Modifier.width(tabletContentWidth),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Bottom,
                                            ) {
                                                GalleryTrainingCard(
                                                    card = currentCard,
                                                    isHintVisible = state.isHintVisible,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onSwipePrevious = if (state.currentIndex > 0) onPreviousClick else null,
                                                    onSwipeNext = if (state.currentIndex < cards.lastIndex) onNextClick else null,
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
                                            card = currentCard,
                                            isHintVisible = state.isHintVisible,
                                            modifier = Modifier.fillMaxWidth(),
                                            onSwipePrevious = if (state.currentIndex > 0) onPreviousClick else null,
                                            onSwipeNext = if (state.currentIndex < cards.lastIndex) onNextClick else null,
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

                        FeedbackOverlay(
                            message = state.feedback?.message,
                            emoji = state.feedback?.emoji,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        if (currentCard != null) {
                            GalleryGlobalNavigationControls(
                                onPreviousClick = if (state.currentIndex > 0) onPreviousClick else null,
                                onNextClick = if (state.currentIndex < cards.lastIndex) onNextClick else null,
                                previousLabel = strings.previous,
                                nextLabel = strings.next,
                                isLandscape = isLandscape,
                                horizontalShift = navigationControlShift,
                                horizontalPadding = navigationControlsHorizontalPadding,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
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
