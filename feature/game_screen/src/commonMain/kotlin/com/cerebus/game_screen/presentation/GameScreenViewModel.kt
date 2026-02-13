package com.cerebus.game_screen.presentation

import androidx.lifecycle.ViewModel
import com.cerebus.game_screen.navigation.GameScreenNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameScreenViewModel(
    private val navigator: GameScreenNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.StartGame)

    val uiState: StateFlow<GameUiState> = _uiState
}