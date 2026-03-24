package com.cerebus.fairy_tales.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cerebus.core.sound_player.SoundClip
import com.cerebus.core.sound_player.SoundSource
import com.cerebus.core.sound_player.createSoundPlayer
import com.cerebus.core.ui.components.FeedbackOverlay
import com.cerebus.core.ui.components.GameLikeActiveScreenShell
import com.cerebus.core.ui.components.GameLikeScreenShell
import com.cerebus.customkeyboard.TrainingKeyboard
import com.cerebus.customkeyboard.resolveShowDigitsRow
import com.cerebus.customkeyboard.resolveTrainingKeyboardHeight
import com.cerebus.fairy_tales.presentation.util.rememberResourceFileCache
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.fairy_tales.generated.resources.Res

private const val TextFadeDurationMillis = 220

@Composable
fun FairyTalesScreenRoute(
    fairyTaleId: String,
    onBackClick: () -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
) {
    val viewModel = koinViewModel<FairyTalesViewModel>(
        parameters = { parametersOf(fairyTaleId) },
    )
    val state = viewModel.state
    val soundPlayer = remember { createSoundPlayer() }
    val resourceFileCache = rememberResourceFileCache()

    DisposableEffect(soundPlayer) {
        onDispose {
            soundPlayer.release()
        }
    }

    LaunchedEffect(viewModel, resourceFileCache, soundPlayer) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FairyTalesEffect.StartStoryPlayback -> {
                    val bytes = runCatching { Res.readBytes(effect.cue.resourcePath) }.getOrNull()
                    if (bytes == null) {
                        viewModel.onStoryPlaybackStarted(
                            playbackToken = effect.playbackToken,
                            audioDurationMillis = null,
                        )
                        return@collect
                    }
                    val fileUri = resourceFileCache.cacheBytes(
                        fileName = effect.cue.fileName,
                        bytes = bytes,
                    )
                    if (fileUri == null) {
                        viewModel.onStoryPlaybackStarted(
                            playbackToken = effect.playbackToken,
                            audioDurationMillis = null,
                        )
                        return@collect
                    }
                    val clip = SoundClip(
                        id = effect.cue.id,
                        source = SoundSource.FileUri(fileUri),
                    )
                    viewModel.onStoryPlaybackStarted(
                        playbackToken = effect.playbackToken,
                        audioDurationMillis = soundPlayer.durationMillis(clip),
                    )
                    soundPlayer.play(clip = clip)
                }
            }
        }
    }

    FairyTalesScreen(
        state = state,
        onBackClick = onBackClick,
        onOpenKeyboardSettings = {
            val studentId = state.studentId
            if (studentId.isNotBlank()) {
                onOpenKeyboardSettings(studentId)
            }
        },
        onShiftChanged = viewModel::onShiftChanged,
        onSymbolPressed = viewModel::onSymbolPressed,
        onBackspacePressed = viewModel::onBackspacePressed,
        onSubmitPressed = viewModel::onSubmitPressed,
        onHintToggle = viewModel::onHintToggle,
        onShowWordToggle = viewModel::onShowWordToggle,
        onSimplifyKeyboardToggle = viewModel::onSimplifyKeyboardToggle,
        onAnimationCompleted = viewModel::onAnimationCompleted,
    )
}

@Composable
fun FairyTalesScreen(
    state: FairyTalesUiState,
    onBackClick: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onShiftChanged: (Boolean) -> Unit,
    onSymbolPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onSubmitPressed: () -> Unit,
    onHintToggle: (Boolean) -> Unit,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
    onAnimationCompleted: (Long) -> Unit,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val isLandscape = windowWidthDp > windowHeightDp
    val isTablet = minOf(windowWidthDp, windowHeightDp) >= 600.dp
    val isPhoneLandscape = isLandscape && !isTablet
    val isSmallScreen = !isTablet
    val showDigitsRow = resolveShowDigitsRow(
        isPhoneLandscape = isPhoneLandscape,
        hideDigitsOnTightScreen = state.hideDigitsOnTightScreen,
        referenceText = state.expectedAnswer,
    )
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
            FairyTalesTopRightHelpChips(
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
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GameLikeActiveScreenShell(
                showKeyboard = true,
                isLandscape = isLandscape,
                keyboardTopPadding = if (isSmallScreen) 4.dp else if (isLandscape) 8.dp else 4.dp,
                keyboard = {
                    TrainingKeyboard(
                        referenceText = state.expectedAnswer,
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
                        .padding(horizontal = if (!isLandscape && !isTablet) 6.dp else if (isPhoneLandscape) 12.dp else if (isLandscape) 20.dp else 16.dp)
                        .padding(
                            top = if (isPhoneLandscape) 18.dp else if (isLandscape) 24.dp else 20.dp,
                            bottom = if (isSmallScreen) 0.dp else if (isLandscape) 12.dp else 16.dp,
                        ),
                ) {
                    if (isLandscape) {
                        FairyTalesLandscapeContent(
                            state = state,
                            availableWidth = maxWidth,
                            isTablet = isTablet,
                            isPhoneLandscape = isPhoneLandscape,
                            onSubmitPressed = onSubmitPressed,
                            onAnimationCompleted = onAnimationCompleted,
                        )
                    } else {
                        FairyTalesPortraitContent(
                            state = state,
                            availableWidth = maxWidth,
                            isTablet = isTablet,
                            onSubmitPressed = onSubmitPressed,
                            onAnimationCompleted = onAnimationCompleted,
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

@Composable
private fun FairyTalesPortraitContent(
    state: FairyTalesUiState,
    availableWidth: Dp,
    isTablet: Boolean,
    onSubmitPressed: () -> Unit,
    onAnimationCompleted: (Long) -> Unit,
) {
    val contentHorizontalPadding = if (isTablet) 32.dp else 6.dp
    val minimumFieldWidth = if (isTablet) 140.dp else 120.dp
    val contentWidth = (availableWidth - contentHorizontalPadding * 2)
        .coerceAtLeast(minimumFieldWidth)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val animationMaxSize = minOf(maxWidth, maxHeight)
                FairyTaleAnimationPanel(
                    state = state,
                    isTablet = isTablet,
                    showCompletedLines = false,
                    onAnimationCompleted = onAnimationCompleted,
                    modifier = Modifier
                        .size(animationMaxSize),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentHorizontalPadding)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.storyLines.isNotEmpty()) {
                FairyTaleCompletedLines(
                    storyLines = state.storyLines,
                    completedCount = state.currentLineIndex.coerceAtMost(state.storyLines.size),
                    isTablet = isTablet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                )
            }

            FairyTalePromptAndInput(
                state = state,
                availableWidth = contentWidth,
                maxContainerWidth = contentWidth,
                minimumFieldWidth = minimumFieldWidth,
                isCompact = !isTablet,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                onSubmitPressed = onSubmitPressed,
            )
        }
    }
}

@Composable
private fun FairyTalesLandscapeContent(
    state: FairyTalesUiState,
    availableWidth: Dp,
    isTablet: Boolean,
    isPhoneLandscape: Boolean,
    onSubmitPressed: () -> Unit,
    onAnimationCompleted: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            FairyTaleAnimationPanel(
                state = state,
                isTablet = isTablet,
                onAnimationCompleted = onAnimationCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isPhoneLandscape) Modifier.widthIn(max = 220.dp) else Modifier
                    ),
            )
        }
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            contentAlignment = if (isPhoneLandscape) Alignment.CenterStart else Alignment.Center,
        ) {
            FairyTalePromptAndInput(
                state = state,
                availableWidth = (availableWidth * 0.6f - if (isPhoneLandscape) 8.dp else 24.dp).coerceAtLeast(200.dp),
                maxContainerWidth = if (isTablet) 560.dp else 340.dp,
                minimumFieldWidth = if (isTablet) 140.dp else 120.dp,
                isCompact = isPhoneLandscape,
                modifier = Modifier.fillMaxWidth(),
                onSubmitPressed = onSubmitPressed,
            )
        }
    }
}

@Composable
private fun FairyTaleAnimationPanel(
    state: FairyTalesUiState,
    isTablet: Boolean,
    showCompletedLines: Boolean = true,
    onAnimationCompleted: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        val animationModifier = if (showCompletedLines) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        } else {
            Modifier.fillMaxSize()
        }
        FairyTaleAnimationSlot(
            animationState = state.animationState,
            onAnimationCompleted = onAnimationCompleted,
            modifier = animationModifier,
        )
        if (showCompletedLines && state.storyLines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FairyTaleCompletedLines(
                storyLines = state.storyLines,
                completedCount = state.currentLineIndex.coerceAtMost(state.storyLines.size),
                isTablet = isTablet,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp, max = 160.dp),
            )
        }
    }
}

@Composable
private fun FairyTalePromptAndInput(
    state: FairyTalesUiState,
    availableWidth: Dp,
    maxContainerWidth: Dp,
    minimumFieldWidth: Dp,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onSubmitPressed: () -> Unit,
) {
    val storyTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 18.sp,
        lineHeight = 20.sp,
    )
    val spacing = if (isCompact) 4.dp else 8.dp
    Column(
        modifier = modifier.widthIn(max = maxContainerWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Crossfade(
            targetState = state.storyText.uppercase().takeIf { state.isHintVisible },
            animationSpec = tween(durationMillis = TextFadeDurationMillis),
            label = "fairy_tale_prompt_text",
        ) { visibleStoryText ->
            if (visibleStoryText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 0.dp,
                            bottom = 0.dp,
                        ),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text(
                            text = visibleStoryText,
                            style = storyTextStyle,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        FairyTalesAnswerSection(
            answerInput = state.answerInput,
            expectedAnswer = state.expectedAnswer,
            inputFeedbackType = state.inputFeedbackType,
            isHintEnabled = state.isInputHintEnabled,
            isShiftEnabled = state.isShiftEnabled,
            isSubmitEnabled = !state.isStoryPlaybackInProgress,
            availableWidth = availableWidth,
            fieldReferenceWidth = availableWidth,
            isStacked = true,
            minimumFieldWidth = minimumFieldWidth,
            isCompact = isCompact,
            onFieldClick = {},
            onSubmit = onSubmitPressed,
        )
    }
}

@Composable
private fun FairyTaleCompletedLines(
    storyLines: List<FairyTaleStoryLine>,
    completedCount: Int,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val textStyle = if (isTablet) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodySmall
    }
    val maxVisibleLines = 4
    val approxLineHeight = if (textStyle.lineHeight != TextUnit.Unspecified) {
        textStyle.lineHeight
    } else {
        textStyle.fontSize * 1.3f
    }
    val maxHeight = with(LocalDensity.current) {
        (approxLineHeight * maxVisibleLines).toDp() + 4.dp
    }
    val approxLineHeightPx = with(LocalDensity.current) {
        approxLineHeight.toPx()
    }

    LaunchedEffect(completedCount, storyLines.size) {
        if (completedCount <= maxVisibleLines) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        val hiddenLinesCount = (completedCount - maxVisibleLines).coerceAtLeast(0)
        val targetScroll = (hiddenLinesCount * approxLineHeightPx)
            .toInt()
            .coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(targetScroll)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        storyLines.forEachIndexed { index, line ->
            val isVisible = index < completedCount
            val lineAlpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(durationMillis = TextFadeDurationMillis),
                label = "fairy_tale_completed_line_alpha",
            )
            Text(
                text = line.text,
                style = textStyle,
                color = Color(0xFF8D8D8D),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .alpha(lineAlpha)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun FairyTaleAnimationSlot(
    animationState: FairyTaleAnimationState,
    onAnimationCompleted: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationAssetPath = animationState.assetPath
    val latestOnAnimationCompleted by rememberUpdatedState(onAnimationCompleted)
    val compositionSpecResult by produceState<Result<LottieCompositionSpec>?>(initialValue = null, key1 = animationAssetPath) {
        value = animationAssetPath?.let { assetPath ->
            runCatching {
                val json = Res.readBytes(assetPath).decodeToString()
                LottieCompositionSpec.JsonString(json)
            }
        }
    }
    val compositionSpec = compositionSpecResult?.getOrNull()
    val compositionResult = compositionSpec?.let { spec ->
        rememberLottieComposition(spec) { spec }
    }
    val composition by (compositionResult ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(null)
    })

    LaunchedEffect(animationState) {
        val playbackState = animationState as? FairyTaleAnimationState.Playback ?: return@LaunchedEffect
        delay(playbackState.totalDurationMillis)
        latestOnAnimationCompleted(playbackState.playbackToken)
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraLarge,
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            animationAssetPath.isNullOrBlank() -> {
                Text(
                    text = "Здесь будет Lottie-анимация",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            composition != null -> {
                Image(
                    painter = rememberLottiePainter(
                        composition = composition,
                        iterations = when (animationState) {
                            FairyTaleAnimationState.None,
                            FairyTaleAnimationState.Idle,
                            FairyTaleAnimationState.Walk -> Compottie.IterateForever
                            is FairyTaleAnimationState.Playback -> animationState.iterations
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            compositionSpecResult?.isFailure == true -> {
                Text(
                    text = "Не удалось загрузить анимацию",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                CircularProgressIndicator()
            }
        }
    }
}
