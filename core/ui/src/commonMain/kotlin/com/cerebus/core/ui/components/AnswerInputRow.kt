package com.cerebus.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnswerInputRow(
    answerInput: String,
    expectedAnswer: String,
    feedbackBorderColor: Color,
    allowMultilineAnswer: Boolean,
    fieldWidth: Dp,
    alignToStart: Boolean,
    minFieldWidth: Dp = fieldWidth,
    adaptiveFieldWidth: Boolean = false,
    buttonSize: Dp = 56.dp,
    buttonSpacing: Dp = 12.dp,
    matchFieldHeight: Boolean = false,
    textScaleOverride: Float? = null,
    revealExpectedAnswer: Boolean = false,
    isShiftEnabled: Boolean = false,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = if (matchFieldHeight) Modifier.height(IntrinsicSize.Min) else Modifier,
        horizontalArrangement = if (alignToStart) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadOnlyAnswerField(
            value = answerInput,
            expectedAnswer = expectedAnswer,
            feedbackBorderColor = feedbackBorderColor,
            allowMultiline = allowMultilineAnswer,
            modifier = if (adaptiveFieldWidth) {
                Modifier.widthIn(min = minFieldWidth, max = fieldWidth)
            } else {
                Modifier.width(fieldWidth)
            },
            textScaleOverride = textScaleOverride,
            adaptiveWidth = adaptiveFieldWidth,
            revealExpectedAnswer = revealExpectedAnswer,
            isShiftEnabled = isShiftEnabled,
            onClick = onFieldClick,
        )
        Button(
            onClick = onSubmit,
            enabled = answerInput.isNotBlank(),
            modifier = Modifier
                .padding(start = buttonSpacing)
                .width(buttonSize)
                .then(
                    if (matchFieldHeight) {
                        Modifier.fillMaxHeight()
                    } else {
                        Modifier.height(buttonSize)
                    }
                ),
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
