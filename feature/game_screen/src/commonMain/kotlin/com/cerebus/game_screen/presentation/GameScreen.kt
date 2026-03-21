package com.cerebus.game_screen.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.cerebus.core.ui.components.GameLikeScreenShell
import com.cerebus.core.ui.components.PracticeModeStatusIcon
import com.cerebus.core.utils.GameLaunchMode
import com.cerebus.game_screen.navigation.GameScreenNavigatorImpl
import com.cerebus.game_screen.presentation.view.FinishedGameContent
import com.cerebus.game_screen.presentation.view.TopRightHelpChips
import com.cerebus.game_screen.presentation.view.active_game.ActiveGameContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.game_screen.generated.resources.Res
import readwriteapp.feature.game_screen.generated.resources.game_practice_mode_description
import readwriteapp.feature.game_screen.generated.resources.game_practice_mode
import readwriteapp.feature.game_screen.generated.resources.game_practice_mode_understood


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
    var showPracticeModeInfo by remember { mutableStateOf(false) }

    GameLikeScreenShell(
        topLeft = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onAction(GameScreenAction.OnCloseClick) },
                ) {
                    Text("✕")
                }

                if (state is GameUiState.Active && state.isPracticeMode) {
                    PracticeModeStatusIcon(
                        onClick = { showPracticeModeInfo = true },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        topRight = {
            if (state is GameUiState.Active) {
                TopRightHelpChips(
                    state = state,
                    onAction = onAction,
                )
            }
        },
    ) {
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
    }

    if (showPracticeModeInfo) {
        AlertDialog(
            onDismissRequest = { showPracticeModeInfo = false },
            title = {
                Text(stringResource(Res.string.game_practice_mode))
            },
            text = {
                Text(stringResource(Res.string.game_practice_mode_description))
            },
            confirmButton = {
                TextButton(onClick = { showPracticeModeInfo = false }) {
                    Text(stringResource(Res.string.game_practice_mode_understood))
                }
            },
        )
    }
}
