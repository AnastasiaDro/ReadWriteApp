package com.cerebus.game_screen.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.cerebus.game_screen.navigation.GameScreenNavigatorImpl
import kotlinx.coroutines.handleCoroutineException
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.game_screen.generated.resources.game_back_to_student
import readwriteapp.feature.game_screen.generated.resources.game_learn_more
import readwriteapp.feature.game_screen.generated.resources.game_learn_more_hint
import readwriteapp.feature.game_screen.generated.resources.game_repeat_last_session_hint
import readwriteapp.feature.game_screen.generated.resources.Res
import readwriteapp.feature.game_screen.generated.resources.game_repeat_last_session

@Composable
fun GameScreenWrapper(
    navController: NavHostController,
    deckIds: List<String>,
) {
    val navigator = remember(navController) { GameScreenNavigatorImpl(navController) }
    val viewModel = koinViewModel<GameScreenViewModel>(
        parameters = { parametersOf(deckIds) }
    )

    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effects.collectAsState()

    LaunchedEffect(effect) {
        when (effect) {
            GameScreenEffect.OpenActiveStudent -> {
                navigator.openActiveStudent()
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
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onAction: (GameScreenAction) -> Unit,
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
                .padding(12.dp),
        ) {
            Text("✕")
        }
    }
}

@Composable
private fun ActiveGameContent(
    state: GameUiState.Active,
    onAction: (GameScreenAction) -> Unit,
) {
    val density = LocalDensity.current
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) { ImageLoader.Builder(platformContext).build() }
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val windowHeightDp = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val cardSize = (minOf(windowHeightDp, windowWidthDp) / 2f).coerceAtLeast(140.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        FeedbackBanner(
            feedback = state.feedback,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Text(
            text = "${state.cardIndex} / ${state.totalCards}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(bottom = 12.dp),
        )

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
            val pictureUrl = state.currentCard.imagePath.orEmpty()
            if (pictureUrl.isNotBlank()) {
                AsyncImage(
                    model = pictureUrl,
                    contentDescription = "Game card image",
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                CardTextFallback(
                    text = state.currentCard.answer,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Hint(
                text = state.currentCard.answer.uppercase(),
                visible = state.isHintVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            val checkButtonWidth = 120.dp
            val buttonSpacing = 12.dp
            val maxFieldWidth = (maxWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(140.dp)
            val fieldWidth = minOf(cardSize, maxFieldWidth)

            val desiredButtonOffset = (fieldWidth / 2) + buttonSpacing + (checkButtonWidth / 2)
            val maxButtonOffset = (maxWidth / 2) - (checkButtonWidth / 2)
            val buttonOffset = minOf(desiredButtonOffset, maxButtonOffset)
            var answerFieldValue by remember(state.currentCard.id) {
                mutableStateOf(
                    TextFieldValue(
                        text = state.answerInput,
                        selection = TextRange(state.answerInput.length),
                    )
                )
            }

            LaunchedEffect(state.answerInput, state.currentCard.id) {
                if (state.answerInput != answerFieldValue.text) {
                    val selectionIndex = answerFieldValue.selection.end.coerceIn(0, state.answerInput.length)
                    answerFieldValue = TextFieldValue(
                        text = state.answerInput,
                        selection = TextRange(selectionIndex),
                    )
                }
            }

            OutlinedTextField(
                value = answerFieldValue,
                onValueChange = { updated ->
                    answerFieldValue = updated
                    if (updated.text != state.answerInput) {
                        onAction(GameScreenAction.OnAnswerChanged(updated.text))
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(fieldWidth)
                    .heightIn(min = 72.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
            )

            Button(
                onClick = { onAction(GameScreenAction.OnCheckClick) },
                enabled = state.answerInput.isNotBlank(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = buttonOffset)
                    .width(checkButtonWidth)
                    .heightIn(min = 72.dp),
            ) {
                Text("Отправить")
            }
        }
    }
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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp),
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
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = feedback?.message ?: "",
                    style = MaterialTheme.typography.titleLarge,
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
            onClick = { onAction(GameScreenAction.OnBackToStudentClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_back_to_student))
        }
    }
}
