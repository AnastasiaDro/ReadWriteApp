package com.cerebus.game_screen.presentation.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cerebus.core.ui.components.ReadOnlyAnswerField as CoreReadOnlyAnswerField
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType

@Composable
fun ReadOnlyAnswerField(
    value: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    allowMultiline: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CoreReadOnlyAnswerField(
        value = value,
        expectedAnswer = expectedAnswer,
        feedbackBorderColor = inputFeedbackType.toFeedbackBorderColor(),
        allowMultiline = allowMultiline,
        modifier = modifier,
        onClick = onClick,
    )
}
