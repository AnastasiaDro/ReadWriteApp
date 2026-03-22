package com.cerebus.game_screen.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.OverlayHelpToggleChip
import com.cerebus.game_screen.presentation.GameScreenAction
import com.cerebus.game_screen.presentation.GameUiState
import com.cerebus.game_screen.presentation.TypingLearningStage

@Composable
fun TopRightHelpChips(
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverlayHelpToggleChip(
                label = "Hint",
                checked = state.isInputHintEnabled,
                wasUsed = state.usedInputHint,
                onClick = {
                    onAction(
                        GameScreenAction.OnInputHintHelpToggled(!state.isInputHintEnabled)
                    )
                },
            )
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
