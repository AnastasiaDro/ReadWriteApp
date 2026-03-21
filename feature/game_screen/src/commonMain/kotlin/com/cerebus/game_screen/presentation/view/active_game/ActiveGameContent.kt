package com.cerebus.game_screen.presentation.view.active_game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import com.cerebus.core.ui.components.AnswerFieldVerticalPadding
import com.cerebus.core.ui.components.FeedbackOverlay
import com.cerebus.core.ui.components.GameLikeActiveScreenShell
import com.cerebus.customkeyboard.TrainingKeyboard
import com.cerebus.customkeyboard.resolveTrainingKeyboardHeight
import com.cerebus.customkeyboard.resolveShowDigitsRow
import com.cerebus.game_screen.presentation.GameScreenAction
import com.cerebus.game_screen.presentation.GameUiState
import com.cerebus.game_screen.presentation.view.AnswerInputRow
import org.jetbrains.compose.resources.stringResource
import readwriteapp.feature.game_screen.generated.resources.Res
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_body
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_confirm
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_dismiss
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_title


@Composable
fun ActiveGameContent(
    state: GameUiState.Active,
    onAction: (GameScreenAction) -> Unit,
    onOpenSessionSettings: (String, Boolean) -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
) {
    val density = LocalDensity.current
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) { ImageLoader.Builder(platformContext).build() }
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val windowHeightDp = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val isLandscape = windowWidthDp > windowHeightDp
    val isTablet = minOf(windowWidthDp, windowHeightDp) >= 600.dp
    val isPhoneLandscape = isLandscape && !isTablet
    val showDigitsRow = resolveShowDigitsRow(
        isPhoneLandscape = isPhoneLandscape,
        hideDigitsOnTightScreen = state.hideDigitsOnTightScreen,
        referenceText = state.currentCard.answer,
    )
    var isKeyboardVisible by remember { mutableStateOf(true) }
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

    GameLikeActiveScreenShell(
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        showKeyboard = isKeyboardVisible,
        isLandscape = isLandscape,
        keyboard = {
            TrainingKeyboard(
                referenceText = state.currentCard.answer,
                activeSymbols = state.activeSymbols,
                isShiftEnabled = state.isShiftEnabled,
                feedbackKey = state.keyboardFeedbackKey,
                feedbackType = state.keyboardFeedbackType,
                onShiftChanged = { onAction(GameScreenAction.OnShiftChanged(it)) },
                onSymbolPressed = { symbol -> onAction(GameScreenAction.OnKeyboardSymbolPressed(symbol)) },
                onBackspacePressed = { onAction(GameScreenAction.OnBackspacePressed) },
                onSpacePressed = {
                    if (!state.answerInput.endsWith(" ")) {
                        onAction(GameScreenAction.OnKeyboardSymbolPressed(" "))
                    }
                },
                onSubmitPressed = { onAction(GameScreenAction.OnCheckClick) },
                onSettingsPressed = {
                    if (state.studentId.isNotBlank()) {
                        onOpenKeyboardSettings(state.studentId)
                    }
                },
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
                        else -> 56.dp
                    },
                    bottom = if (isLandscape) 0.dp else 12.dp,
                ),
        ) {
            val compactMode = isKeyboardVisible || isLandscape
            val availableContentWidth = maxWidth
            val availableContentHeight = maxHeight
            val allowMultilineAnswer = state.currentCard.answer.length > 10 || state.currentCard.answer.contains(' ')
            val answerTextScale = if (isTablet) 2f else 1.5f
            val useStackedInput = isLandscape || maxWidth < 420.dp
            val inputSectionHeight = resolveAnswerFieldMinHeight(
                baseLineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                textScale = answerTextScale,
                allowMultiline = allowMultilineAnswer,
                density = density,
            )
            val verticalSpacing = if (compactMode) 12.dp else 18.dp
            val landscapeCardSize = minOf(
                availableContentHeight - 8.dp,
                availableContentWidth,
            ).coerceAtLeast(120.dp)
            val landscapeHalfWidth = if (isLandscape) {
                ((availableContentWidth - 8.dp) / 2f).coerceAtLeast(140.dp)
            } else {
                0.dp
            }
            val landscapeCounterWidth = 28.dp
            val effectiveLandscapeCardSize = landscapeCardSize
            val effectiveLandscapeInputWidth = landscapeHalfWidth

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLandscape && isKeyboardVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize(),
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
                                    PracticeModeBadge(
                                        visible = state.isPracticeMode,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        Text(
                                            text = "${state.cardIndex}/${state.totalCards}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.width(landscapeCounterWidth),
                                            textAlign = TextAlign.Center,
                                        )
                                        Box(modifier = Modifier.width(6.dp))
                                        GameCard(
                                            cardSize = effectiveLandscapeCardSize,
                                            imagePath = state.currentCard.imagePath.orEmpty(),
                                            answer = state.currentCard.answer,
                                            isHintVisible = state.isHintVisible,
                                            imageLoader = imageLoader,
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                Box(
                                    modifier = Modifier.wrapContentWidth(),
                                    contentAlignment = Alignment.BottomStart,
                                ) {
                                    AnswerInputSection(
                                        answerInput = state.answerInput,
                                        expectedAnswer = state.currentCard.answer,
                                        inputFeedbackType = state.inputFeedbackType,
                                        isStacked = false,
                                        availableWidth = minOf(effectiveLandscapeInputWidth, 280.dp),
                                        fieldReferenceWidth = effectiveLandscapeCardSize,
                                        alignToStart = true,
                                        onFieldClick = { isKeyboardVisible = true },
                                        onSubmit = { onAction(GameScreenAction.OnCheckClick) },
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    text = "${state.cardIndex}/${state.totalCards}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(end = 8.dp),
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
                                    val tabletAnswerRowHeight = resolveAnswerFieldMinHeight(
                                        baseLineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                                        textScale = 2f,
                                        allowMultiline = allowMultilineAnswer,
                                        density = density,
                                    )
                                    val tabletContentWidth = minOf(
                                        maxWidth,
                                        maxHeight - tabletAnswerRowHeight - 10.dp,
                                    ).coerceAtLeast(120.dp)

                                    Column(
                                        modifier = Modifier.fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                    ) {
                                        PracticeModeBadge(
                                            visible = state.isPracticeMode,
                                        )
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.BottomCenter,
                                        ) {
                                            Column(
                                                modifier = Modifier.width(tabletContentWidth),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                                            ) {
                                                GameCard(
                                                    cardSize = tabletContentWidth,
                                                    imagePath = state.currentCard.imagePath.orEmpty(),
                                                    answer = state.currentCard.answer,
                                                    isHintVisible = state.isHintVisible,
                                                    imageLoader = imageLoader,
                                                )
                                                AnswerInputRow(
                                                    answerInput = state.answerInput,
                                                    expectedAnswer = state.currentCard.answer,
                                                    inputFeedbackType = state.inputFeedbackType,
                                                    allowMultilineAnswer = allowMultilineAnswer,
                                                    fieldWidth = (tabletContentWidth - 68.dp)
                                                        .coerceAtLeast(112.dp),
                                                    alignToStart = false,
                                                    onFieldClick = { isKeyboardVisible = true },
                                                    onSubmit = { onAction(GameScreenAction.OnCheckClick) },
                                                )
                                            }
                                        }
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

                    FeedbackOverlay(
                        message = state.feedback?.message,
                        emoji = state.feedback?.emoji,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (isTablet) 30.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    ) {
                        PracticeModeBadge(
                            visible = state.isPracticeMode,
                        )
                        Text(
                            text = "${state.cardIndex} / ${state.totalCards}",
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
                                modifier = Modifier.width(portraitContentWidth),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                            ) {
                                GameCard(
                                    cardSize = portraitContentWidth,
                                    imagePath = state.currentCard.imagePath.orEmpty(),
                                    answer = state.currentCard.answer,
                                    isHintVisible = state.isHintVisible,
                                    imageLoader = imageLoader,
                                )

                                AnswerInputSection(
                                    answerInput = state.answerInput,
                                    expectedAnswer = state.currentCard.answer,
                                    inputFeedbackType = state.inputFeedbackType,
                                    isStacked = useStackedInput,
                                    availableWidth = portraitContentWidth,
                                    fieldReferenceWidth = portraitContentWidth,
                                    onFieldClick = { isKeyboardVisible = true },
                                    onSubmit = { onAction(GameScreenAction.OnCheckClick) },
                                )
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

    if (state.showTypoSettingsSuggestion && state.studentId.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { onAction(GameScreenAction.OnTypoSuggestionDismissed) },
            title = {
                Text(stringResource(Res.string.game_typo_settings_suggestion_title))
            },
            text = {
                Text(stringResource(Res.string.game_typo_settings_suggestion_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(GameScreenAction.OnTypoSuggestionDismissed)
                        onOpenSessionSettings(state.studentId, true)
                    },
                ) {
                    Text(stringResource(Res.string.game_typo_settings_suggestion_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(GameScreenAction.OnTypoSuggestionDismissed) },
                ) {
                    Text(stringResource(Res.string.game_typo_settings_suggestion_dismiss))
                }
            },
        )
    }
}

private fun resolveAnswerFieldMinHeight(
    baseLineHeight: TextUnit,
    textScale: Float,
    allowMultiline: Boolean,
    density: Density,
): Dp {
    val lineCount = if (allowMultiline) 2 else 1
    val scaledLineHeight = with(density) { (baseLineHeight * textScale).toDp() }
    return scaledLineHeight * lineCount + (AnswerFieldVerticalPadding * 2)
}
