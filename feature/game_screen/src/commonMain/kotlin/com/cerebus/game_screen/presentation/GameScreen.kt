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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.Dp
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
import readwriteapp.feature.game_screen.generated.resources.game_help_show_word
import readwriteapp.feature.game_screen.generated.resources.game_help_simplify_keyboard
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

        TextButton(
            onClick = { onAction(GameScreenAction.OnCloseClick) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp),
        ) {
            Text("✕")
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
                    top = if (isPhoneLandscape) 20.dp else 56.dp,
                    bottom = 12.dp,
                ),
        ) {
            val compactMode = isKeyboardVisible || isLandscape
            val useLandscapeFeedbackOverlay = isLandscape
            val showShowWordToggle = state.learningStage == TypingLearningStage.Recall
            val showSimplifyToggle = true
            val helpToggleCount = (if (showShowWordToggle) 1 else 0) + (if (showSimplifyToggle) 1 else 0)
            val availableContentWidth = maxWidth
            val availableContentHeight = maxHeight
            val feedbackReservedHeight = when {
                useLandscapeFeedbackOverlay -> 0.dp
                state.feedback == null -> 0.dp
                compactMode -> 72.dp
                else -> 126.dp
            }
            val useStackedInput = isLandscape || maxWidth < 420.dp
            val inputSectionHeight = when {
                useStackedInput && helpToggleCount > 0 -> if (helpToggleCount > 1) 196.dp else 152.dp
                useStackedInput -> 124.dp
                helpToggleCount > 0 -> if (helpToggleCount > 1) 128.dp else 104.dp
                else -> 56.dp
            }
            val counterHeight = 24.dp
            val verticalSpacing = if (compactMode) 12.dp else 18.dp
            val portraitCardSize = minOf(
                availableContentWidth * if (compactMode) 0.62f else 0.72f,
                availableContentHeight - feedbackReservedHeight - inputSectionHeight - counterHeight - (verticalSpacing * 3),
            ).coerceAtLeast(120.dp)
            val landscapeCardSize = minOf(
                availableContentHeight - feedbackReservedHeight - counterHeight - 20.dp,
                availableContentWidth * 0.42f,
            ).coerceAtLeast(120.dp)
            val landscapeInputWidth = minOf(
                availableContentWidth * 0.34f,
                320.dp,
            ).coerceAtLeast(220.dp)
            val landscapeBlockSpacing = minOf(
                availableContentWidth * 0.04f,
                24.dp,
            ).coerceAtLeast(12.dp)
            val effectiveLandscapeBlockSpacing = if (isPhoneLandscape) 8.dp else landscapeBlockSpacing
            val phoneLandscapeHalfWidth = if (isPhoneLandscape) {
                ((availableContentWidth - effectiveLandscapeBlockSpacing) / 2f).coerceAtLeast(140.dp)
            } else {
                0.dp
            }
            val effectiveLandscapeCardSize = if (isPhoneLandscape) {
                minOf(
                    landscapeCardSize,
                    (phoneLandscapeHalfWidth - 48.dp).coerceAtLeast(120.dp),
                )
            } else {
                landscapeCardSize
            }
            val effectiveLandscapeInputWidth = if (isPhoneLandscape) {
                phoneLandscapeHalfWidth
            } else {
                landscapeInputWidth
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (!useLandscapeFeedbackOverlay) {
                    FeedbackBanner(
                        feedback = state.feedback,
                        compact = compactMode,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                    )
                }

                if (isLandscape && isKeyboardVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = feedbackReservedHeight),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = if (isPhoneLandscape) Alignment.Top else Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = if (isPhoneLandscape) {
                                Modifier.width(phoneLandscapeHalfWidth)
                            } else {
                                Modifier.width(landscapeCardSize + 12.dp)
                            },
                        ) {
                            Column(
                                modifier = Modifier.align(if (isPhoneLandscape) Alignment.TopEnd else Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = if (isPhoneLandscape) Arrangement.Top else Arrangement.Center,
                            ) {
                                PracticeModeBadge(
                                    visible = state.isPracticeMode,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                if (isTablet) {
                                    Text(
                                        text = "${state.cardIndex} / ${state.totalCards}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    GameCard(
                                        cardSize = landscapeCardSize,
                                        imagePath = state.currentCard.imagePath.orEmpty(),
                                        answer = state.currentCard.answer,
                                        isHintVisible = state.isHintVisible,
                                        imageLoader = imageLoader,
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "${state.cardIndex}/${state.totalCards}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.Center,
                                        )
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
                        }

                        Box(modifier = Modifier.width(effectiveLandscapeBlockSpacing))

                        if (isPhoneLandscape) {
                            Box(
                                modifier = Modifier.width(phoneLandscapeHalfWidth),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                Column(
                                    modifier = Modifier.widthIn(max = phoneLandscapeHalfWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top,
                                ) {
                                    AnswerInputSection(
                                        answerInput = state.answerInput,
                                        expectedAnswer = state.currentCard.answer,
                                        inputFeedbackType = state.inputFeedbackType,
                                        showShowWordToggle = showShowWordToggle,
                                        showSimplifyToggle = showSimplifyToggle,
                                        isShowWordEnabled = state.isHintVisible,
                                        isSimplifiedKeyboardEnabled = state.isSimplifiedKeyboardEnabled,
                                        usedShowWord = state.usedShowWord,
                                        usedSimplifiedKeyboard = state.usedSimplifiedKeyboard,
                                        isStacked = true,
                                        availableWidth = effectiveLandscapeInputWidth,
                                        fieldReferenceWidth = effectiveLandscapeCardSize,
                                        onFieldClick = { isKeyboardVisible = true },
                                        onSubmit = { onAction(GameScreenAction.OnCheckClick) },
                                        onShowWordToggle = { onAction(GameScreenAction.OnShowWordHelpToggled(it)) },
                                        onSimplifyKeyboardToggle = {
                                            onAction(GameScreenAction.OnSimplifyKeyboardHelpToggled(it))
                                        },
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.width(landscapeInputWidth),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AnswerInputSection(
                                    answerInput = state.answerInput,
                                    expectedAnswer = state.currentCard.answer,
                                    inputFeedbackType = state.inputFeedbackType,
                                    showShowWordToggle = showShowWordToggle,
                                    showSimplifyToggle = showSimplifyToggle,
                                    isShowWordEnabled = state.isHintVisible,
                                    isSimplifiedKeyboardEnabled = state.isSimplifiedKeyboardEnabled,
                                    usedShowWord = state.usedShowWord,
                                    usedSimplifiedKeyboard = state.usedSimplifiedKeyboard,
                                    isStacked = true,
                                    availableWidth = effectiveLandscapeInputWidth,
                                    fieldReferenceWidth = effectiveLandscapeCardSize,
                                    onFieldClick = { isKeyboardVisible = true },
                                    onSubmit = { onAction(GameScreenAction.OnCheckClick) },
                                    onShowWordToggle = { onAction(GameScreenAction.OnShowWordHelpToggled(it)) },
                                    onSimplifyKeyboardToggle = {
                                        onAction(GameScreenAction.OnSimplifyKeyboardHelpToggled(it))
                                    },
                                )
                            }
                        }
                    }

                    LandscapeFeedbackOverlay(
                        feedback = state.feedback,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = feedbackReservedHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        PracticeModeBadge(
                            visible = state.isPracticeMode,
                        )

                        Text(
                            text = "${state.cardIndex} / ${state.totalCards}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        GameCard(
                            cardSize = portraitCardSize,
                            imagePath = state.currentCard.imagePath.orEmpty(),
                            answer = state.currentCard.answer,
                            isHintVisible = state.isHintVisible,
                            imageLoader = imageLoader,
                        )

                        AnswerInputSection(
                            answerInput = state.answerInput,
                            expectedAnswer = state.currentCard.answer,
                            inputFeedbackType = state.inputFeedbackType,
                            showShowWordToggle = showShowWordToggle,
                            showSimplifyToggle = showSimplifyToggle,
                            isShowWordEnabled = state.isHintVisible,
                            isSimplifiedKeyboardEnabled = state.isSimplifiedKeyboardEnabled,
                            usedShowWord = state.usedShowWord,
                            usedSimplifiedKeyboard = state.usedSimplifiedKeyboard,
                            isStacked = useStackedInput,
                            availableWidth = availableContentWidth,
                            fieldReferenceWidth = portraitCardSize,
                            onFieldClick = { isKeyboardVisible = true },
                            onSubmit = { onAction(GameScreenAction.OnCheckClick) },
                            onShowWordToggle = { onAction(GameScreenAction.OnShowWordHelpToggled(it)) },
                            onSimplifyKeyboardToggle = {
                                onAction(GameScreenAction.OnSimplifyKeyboardHelpToggled(it))
                            },
                        )
                    }
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
                .padding(horizontal = 6.dp, vertical = 4.dp),
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
private fun LandscapeFeedbackOverlay(
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
    showShowWordToggle: Boolean,
    showSimplifyToggle: Boolean,
    isShowWordEnabled: Boolean,
    isSimplifiedKeyboardEnabled: Boolean,
    usedShowWord: Boolean,
    usedSimplifiedKeyboard: Boolean,
    isStacked: Boolean,
    availableWidth: Dp,
    fieldReferenceWidth: Dp,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
) {
    val checkButtonWidth = 120.dp
    val buttonSpacing = 12.dp
    val maxFieldWidth = (availableWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(140.dp)
    val fieldWidth = if (isStacked) {
        maxFieldWidth
    } else {
        minOf(fieldReferenceWidth, maxFieldWidth)
    }
    val allowMultilineAnswer = expectedAnswer.length > 10 || expectedAnswer.contains(' ')
    val answerFieldHeight = if (allowMultilineAnswer) 84.dp else 56.dp

    if (isStacked) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ((availableWidth - fieldWidth) / 2).coerceAtLeast(0.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReadOnlyAnswerField(
                value = answerInput,
                expectedAnswer = expectedAnswer,
                inputFeedbackType = inputFeedbackType,
                allowMultiline = allowMultilineAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(answerFieldHeight),
                onClick = onFieldClick,
            )

            Button(
                onClick = onSubmit,
                enabled = answerInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Отправить")
            }

            if (showShowWordToggle || showSimplifyToggle) {
                HelpTogglesSection(
                    showShowWordToggle = showShowWordToggle,
                    showSimplifyToggle = showSimplifyToggle,
                    isShowWordEnabled = isShowWordEnabled,
                    isSimplifiedKeyboardEnabled = isSimplifiedKeyboardEnabled,
                    usedShowWord = usedShowWord,
                    usedSimplifiedKeyboard = usedSimplifiedKeyboard,
                    modifier = Modifier.fillMaxWidth(),
                    onShowWordToggle = onShowWordToggle,
                    onSimplifyKeyboardToggle = onSimplifyKeyboardToggle,
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReadOnlyAnswerField(
                    value = answerInput,
                    expectedAnswer = expectedAnswer,
                    inputFeedbackType = inputFeedbackType,
                    allowMultiline = allowMultilineAnswer,
                    modifier = Modifier
                        .width(fieldWidth)
                        .height(answerFieldHeight),
                    onClick = onFieldClick,
                )
                Button(
                    onClick = onSubmit,
                    enabled = answerInput.isNotBlank(),
                    modifier = Modifier
                        .padding(start = buttonSpacing)
                        .width(checkButtonWidth)
                        .height(56.dp),
                ) {
                    Text("Отправить")
                }
            }

            if (showShowWordToggle || showSimplifyToggle) {
                HelpTogglesSection(
                    showShowWordToggle = showShowWordToggle,
                    showSimplifyToggle = showSimplifyToggle,
                    isShowWordEnabled = isShowWordEnabled,
                    isSimplifiedKeyboardEnabled = isSimplifiedKeyboardEnabled,
                    usedShowWord = usedShowWord,
                    usedSimplifiedKeyboard = usedSimplifiedKeyboard,
                    modifier = Modifier.width(fieldWidth + checkButtonWidth + buttonSpacing),
                    onShowWordToggle = onShowWordToggle,
                    onSimplifyKeyboardToggle = onSimplifyKeyboardToggle,
                )
            }
        }
    }
}

@Composable
private fun HelpTogglesSection(
    showShowWordToggle: Boolean,
    showSimplifyToggle: Boolean,
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
    ) {
        if (showShowWordToggle) {
            HelpToggleRow(
                label = stringResource(Res.string.game_help_show_word),
                checked = isShowWordEnabled,
                wasUsed = usedShowWord,
                onCheckedChange = onShowWordToggle,
            )
        }

        if (showSimplifyToggle) {
            HelpToggleRow(
                label = stringResource(Res.string.game_help_simplify_keyboard),
                checked = isSimplifiedKeyboardEnabled,
                wasUsed = usedSimplifiedKeyboard,
                onCheckedChange = onSimplifyKeyboardToggle,
            )
        }
    }
}

@Composable
private fun HelpToggleRow(
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
private fun ReadOnlyAnswerField(
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
private fun FeedbackBanner(
    feedback: FeedbackUi?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (feedback == null) 0.dp else if (compact) 72.dp else 126.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(
                initialAlpha = 0f,
                animationSpec = tween(durationMillis = 500),
            ),
            exit = fadeOut(
                targetAlpha = 0f,
                animationSpec = tween(durationMillis = 500),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = feedback?.emoji ?: "",
                    style = if (compact) {
                        MaterialTheme.typography.headlineLarge
                    } else {
                        MaterialTheme.typography.displayLarge
                    },
                )
                Text(
                    text = feedback?.message ?: "",
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
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
