package com.cerebus.game_screen.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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