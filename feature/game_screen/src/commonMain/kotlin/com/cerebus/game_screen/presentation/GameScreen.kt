package com.cerebus.game_screen.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.cerebus.game_screen.navigation.GameScreenNavigatorImpl
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun GameScreenWrapper(navController: NavHostController) {

    val navigator = GameScreenNavigatorImpl(navController)
    val viewModel = koinViewModel<GameScreenViewModel>(
        parameters = { parametersOf(navigator) }
    )

    val state = viewModel.uiState.collectAsState()

}

@Composable
public fun GameScreen(state: GameUiState) {

}
