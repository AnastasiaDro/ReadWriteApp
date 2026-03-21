package com.cerebus.game_screen.presentation.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.cerebus.core.ui.components.AnswerInputRow as CoreAnswerInputRow
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType

@Composable
fun AnswerInputRow(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    allowMultilineAnswer: Boolean,
    fieldWidth: Dp,
    alignToStart: Boolean,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    CoreAnswerInputRow(
        answerInput = answerInput,
        expectedAnswer = expectedAnswer,
        feedbackBorderColor = inputFeedbackType.toFeedbackBorderColor(),
        allowMultilineAnswer = allowMultilineAnswer,
        fieldWidth = fieldWidth,
        alignToStart = alignToStart,
        onFieldClick = onFieldClick,
        onSubmit = onSubmit,
    )
}

internal fun TrainingKeyboardFeedbackType?.toFeedbackBorderColor(): Color =
    when (this) {
        TrainingKeyboardFeedbackType.Correct -> Color(0xFF9AD88F)
        TrainingKeyboardFeedbackType.Slip -> Color(0xFFFFD35C)
        TrainingKeyboardFeedbackType.Wrong -> Color(0xFFFF7A7A)
        null -> Color.Unspecified
    }
