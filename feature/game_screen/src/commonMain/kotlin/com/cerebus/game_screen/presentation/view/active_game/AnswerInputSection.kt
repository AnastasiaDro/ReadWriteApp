package com.cerebus.game_screen.presentation.view.active_game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.game_screen.presentation.view.AnswerInputRow


@Composable
fun AnswerInputSection(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    isInputHintEnabled: Boolean,
    isStacked: Boolean,
    availableWidth: Dp,
    fieldReferenceWidth: Dp,
    alignToStart: Boolean = false,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val checkButtonWidth = 56.dp
    val buttonSpacing = 12.dp
    val maxFieldWidth = (availableWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(140.dp)
    val baseFieldWidth = if (isStacked) maxFieldWidth else minOf(fieldReferenceWidth, maxFieldWidth)
    val allowMultilineAnswer = expectedAnswer.length > 10 || expectedAnswer.contains(' ')
    val wordCount = expectedAnswer.trim()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }
    val shouldExpandForSingleWord = wordCount == 1
    val fieldWidth = if (shouldExpandForSingleWord) maxFieldWidth else baseFieldWidth

    Column(
        modifier = if (alignToStart) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignToStart) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnswerInputRow(
            answerInput = answerInput,
            expectedAnswer = expectedAnswer,
            inputFeedbackType = inputFeedbackType,
            allowMultilineAnswer = allowMultilineAnswer,
            fieldWidth = fieldWidth,
            alignToStart = alignToStart,
            minFieldWidth = baseFieldWidth,
            adaptiveFieldWidth = shouldExpandForSingleWord,
            multilineMaxLines = 3,
            revealExpectedAnswer = isInputHintEnabled,
            onFieldClick = onFieldClick,
            onSubmit = onSubmit,
        )
    }
}
