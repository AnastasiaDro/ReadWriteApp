package com.cerebus.game_screen.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.cerebus.customkeyboard.TrainingKeyboard
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.customkeyboard.resolveTrainingKeyboardHeight
import com.cerebus.core.utils.GameLaunchMode
import com.cerebus.game_screen.navigation.GameScreenNavigatorImpl
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.game_screen.generated.resources.game_back_to_student
import readwriteapp.feature.game_screen.generated.resources.game_learn_more
import readwriteapp.feature.game_screen.generated.resources.game_learn_more_hint
import readwriteapp.feature.game_screen.generated.resources.game_random_review
import readwriteapp.feature.game_screen.generated.resources.game_random_review_hint
import readwriteapp.feature.game_screen.generated.resources.game_repeat_last_session_hint
import readwriteapp.feature.game_screen.generated.resources.game_practice_mode
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_body
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_confirm
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_dismiss
import readwriteapp.feature.game_screen.generated.resources.game_typo_settings_suggestion_title
import readwriteapp.feature.game_screen.generated.resources.Res
import readwriteapp.feature.game_screen.generated.resources.game_repeat_last_session

@Composable
fun GameScreenWrapper(
    navController: NavHostController,
    deckIds: List<String>,
    launchMode: GameLaunchMode,
    onOpenSessionSettings: (String, Boolean) -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
) {
    val navigator = remember(navController) { GameScreenNavigatorImpl(navController) }
    val viewModel = koinViewModel<GameScreenViewModel>(
        parameters = { parametersOf(deckIds, launchMode) }
    )

    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effects.collectAsState()

    LaunchedEffect(effect) {
        val currentEffect = effect
        when (currentEffect) {
            GameScreenEffect.OpenActiveStudent -> {
                navigator.openActiveStudent()
                viewModel.consumeEffect()
            }
            is GameScreenEffect.OpenSessionSettings -> {
                onOpenSessionSettings(currentEffect.studentId, true)
                viewModel.consumeEffect()
            }
            GameScreenEffect.CloseGame -> {
                navigator.closeGame()
                viewModel.consumeEffect()
            }
            null -> Unit
        }
    }

    GameScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSessionSettings = onOpenSessionSettings,
        onOpenKeyboardSettings = onOpenKeyboardSettings,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onAction: (GameScreenAction) -> Unit,
    onOpenSessionSettings: (String, Boolean) -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            GameUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Загрузка...",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            is GameUiState.Active -> {
                ActiveGameContent(
                    state = state,
                    onAction = onAction,
                    onOpenSessionSettings = onOpenSessionSettings,
                    onOpenKeyboardSettings = onOpenKeyboardSettings,
                )
            }

            is GameUiState.Finished -> {
                FinishedGameContent(
                    state = state,
                    onAction = onAction,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 12.dp, top = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            TextButton(
                onClick = { onAction(GameScreenAction.OnCloseClick) },
            ) {
                Text("✕")
            }

            if (state is GameUiState.Active) {
                TopRightHelpChips(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ActiveGameContent(
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
    val showDigitsRow = !(
        isPhoneLandscape &&
            state.hideDigitsOnTightScreen &&
            state.currentCard.answer.none { it.isDigit() }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(
                    top = if (isLandscape) 20.dp else 56.dp,
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
            val counterHeight = 24.dp
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
                        feedback = state.feedback,
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
                        feedback = state.feedback,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isKeyboardVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    top = if (isLandscape) 8.dp else 4.dp,
                    bottom = 4.dp,
                ),
        ) {
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
}

@Composable
private fun TopRightHelpChips(
    state: GameUiState.Active,
    onAction: (GameScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showWordChip = state.learningStage == TypingLearningStage.Recall

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (showWordChip) {
            OverlayHelpToggleChip(
                label = "Word",
                checked = state.isHintVisible,
                wasUsed = state.usedShowWord,
                onClick = {
                    onAction(GameScreenAction.OnShowWordHelpToggled(!state.isHintVisible))
                },
            )
        }
        OverlayHelpToggleChip(
            label = "Aa",
            checked = state.isSimplifiedKeyboardEnabled,
            wasUsed = state.usedSimplifiedKeyboard,
            onClick = {
                onAction(
                    GameScreenAction.OnSimplifyKeyboardHelpToggled(
                        !state.isSimplifiedKeyboardEnabled
                    )
                )
            },
        )
    }
}

@Composable
private fun PracticeModeBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)),
    ) {
        Text(
            text = stringResource(Res.string.game_practice_mode),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FeedbackOverlay(
    feedback: FeedbackUi?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = feedback != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = feedback?.emoji.orEmpty(),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = feedback?.message.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun GameCard(
    cardSize: Dp,
    imagePath: String,
    answer: String,
    isHintVisible: Boolean,
    imageLoader: ImageLoader,
) {
    var imageLoadFailed by remember(imagePath) { mutableStateOf(false) }
    val normalizedImagePath = imagePath.trim()
    val isTextCard = normalizedImagePath.isBlank() || imageLoadFailed

    Box(
        modifier = Modifier
            .size(cardSize)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (isTextCard) {
            CardTextFallback(
                text = answer,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = normalizedImagePath,
                contentDescription = "Game card image",
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { imageLoadFailed = false },
                onError = { imageLoadFailed = true },
            )
        }

        Hint(
            text = answer.uppercase(),
            visible = isHintVisible && !isTextCard,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun AnswerInputSection(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    isStacked: Boolean,
    availableWidth: Dp,
    fieldReferenceWidth: Dp,
    alignToStart: Boolean = false,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val checkButtonWidth = 120.dp
    val buttonSpacing = 12.dp
    val maxFieldWidth = (availableWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(140.dp)
    val fieldWidth = if (isStacked) maxFieldWidth else minOf(fieldReferenceWidth, maxFieldWidth)

    Column(
        modifier = if (alignToStart) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignToStart) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnswerInputRow(
            answerInput = answerInput,
            expectedAnswer = expectedAnswer,
            inputFeedbackType = inputFeedbackType,
            fieldWidth = fieldWidth,
            alignToStart = alignToStart,
            onFieldClick = onFieldClick,
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun AnswerInputRow(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    fieldWidth: Dp,
    alignToStart: Boolean,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val allowMultilineAnswer = expectedAnswer.length > 10 || expectedAnswer.contains(' ')

    Row(
        horizontalArrangement = if (alignToStart) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadOnlyAnswerField(
            value = answerInput,
            expectedAnswer = expectedAnswer,
            inputFeedbackType = inputFeedbackType,
            allowMultiline = allowMultilineAnswer,
            modifier = Modifier
                .width(fieldWidth),
            onClick = onFieldClick,
        )
        Button(
            onClick = onSubmit,
            enabled = answerInput.isNotBlank(),
            modifier = Modifier
                .padding(start = 12.dp)
                .width(56.dp)
                .height(56.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(
                horizontal = 8.dp,
                vertical = 8.dp,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Отправить",
                modifier = Modifier.offset(x = 1.dp),
            )
        }
    }
}

@Composable
private fun OverlayHelpToggleChip(
    label: String,
    checked: Boolean,
    wasUsed: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        checked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    val contentColor = when {
        checked -> MaterialTheme.colorScheme.onPrimary
        wasUsed -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReadOnlyAnswerField(
    value: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    allowMultiline: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val windowHeightDp = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val isTablet = minOf(windowWidthDp, windowHeightDp) >= 600.dp
    val baseTextStyle = MaterialTheme.typography.bodyLarge
    val answerTextScale = if (isTablet) 2f else 1.5f
    val answerTextStyle = baseTextStyle.copy(
        fontSize = baseTextStyle.fontSize * answerTextScale,
        lineHeight = baseTextStyle.lineHeight * answerTextScale,
    )
    val minFieldHeight = resolveAnswerFieldMinHeight(
        baseLineHeight = baseTextStyle.lineHeight,
        textScale = answerTextScale,
        allowMultiline = allowMultiline,
        density = density,
    )
    val currentSlotBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
    val currentSlotTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    val displayedValue = remember(
        value,
        expectedAnswer,
        currentSlotBackgroundColor,
        currentSlotTextColor,
    ) {
        buildAnswerProgressMask(
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

    Box(modifier = modifier.heightIn(min = minFieldHeight)) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxSize(),
            textStyle = answerTextStyle,
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
            style = answerTextStyle,
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

private fun resolveAnswerFieldMinHeight(
    baseLineHeight: TextUnit,
    textScale: Float,
    allowMultiline: Boolean,
    density: Density,
): Dp {
    val lineCount = if (allowMultiline) 2 else 1
    val scaledLineHeight = with(density) { (baseLineHeight * textScale).toDp() }
    return scaledLineHeight * lineCount
}

private fun buildAnswerProgressMask(
    answerInput: String,
    expectedAnswer: String,
    currentSlotBackgroundColor: Color,
    currentSlotTextColor: Color,
): AnnotatedString {
    if (expectedAnswer.isEmpty()) return AnnotatedString(answerInput)
    return buildAnnotatedString {
        val currentSlotIndex = nextVisibleSlotIndex(
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

private fun nextVisibleSlotIndex(
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
private fun CardTextFallback(
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .fillMaxWidth(),
    )
}

@Composable
private fun Hint(
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
private fun FinishedGameContent(
    state: GameUiState.Finished,
    onAction: (GameScreenAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Вы завершили колоду",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "\"${state.deckTitle}\"",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )

        Text(
            text = "${state.correctAnswers} / ${state.totalCards} правильных ответов",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnLearnMoreClick) },
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_learn_more))
        }

        Text(
            text = stringResource(Res.string.game_learn_more_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnRetryClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_repeat_last_session))
        }

        Text(
            text = stringResource(Res.string.game_repeat_last_session_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnRandomReviewClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_random_review))
        }

        Text(
            text = stringResource(Res.string.game_random_review_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnBackToStudentClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_back_to_student))
        }
    }
}
