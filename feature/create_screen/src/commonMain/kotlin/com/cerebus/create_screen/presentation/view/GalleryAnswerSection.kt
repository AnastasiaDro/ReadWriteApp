package com.cerebus.create_screen.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.AnswerFieldVerticalPadding
import com.cerebus.core.ui.components.AnswerInputRow
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType

@Composable
internal fun GalleryGameLikeAnswerSection(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    availableWidth: Dp,
    fieldReferenceWidth: Dp,
    isStacked: Boolean,
    isCompact: Boolean = false,
    isAdaptiveWidth: Boolean = false,
    minimumFieldWidth: Dp = 140.dp,
    modifier: Modifier = Modifier,
    alignToStart: Boolean = false,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val checkButtonWidth = if (isCompact) 48.dp else 56.dp
    val buttonSpacing = if (isCompact) 8.dp else 12.dp
    val maxFieldWidth = (availableWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(minimumFieldWidth)
    val fieldWidth = when {
        isStacked || isAdaptiveWidth -> maxFieldWidth
        else -> minOf(fieldReferenceWidth, maxFieldWidth)
    }
    val allowMultilineAnswer = if (isCompact) {
        false
    } else {
        expectedAnswer.length > 10 || expectedAnswer.contains(' ')
    }

    Column(
        modifier = modifier.then(
            if (alignToStart || isAdaptiveWidth) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
        ),
        horizontalAlignment = if (alignToStart) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnswerInputRow(
            answerInput = answerInput,
            expectedAnswer = expectedAnswer,
            feedbackBorderColor = when (inputFeedbackType) {
                TrainingKeyboardFeedbackType.Correct -> Color(0xFF9AD88F)
                TrainingKeyboardFeedbackType.Slip -> Color(0xFFFFD35C)
                TrainingKeyboardFeedbackType.Wrong -> Color(0xFFFF7A7A)
                null -> Color.Unspecified
            },
            allowMultilineAnswer = allowMultilineAnswer,
            fieldWidth = fieldWidth,
            alignToStart = alignToStart,
            minFieldWidth = minimumFieldWidth,
            adaptiveFieldWidth = isAdaptiveWidth,
            buttonSize = checkButtonWidth,
            buttonSpacing = buttonSpacing,
            textScaleOverride = if (isCompact) 1f else null,
            onFieldClick = onFieldClick,
            onSubmit = onSubmit,
        )
    }
}

internal fun resolveGalleryAnswerSectionMinHeight(
    baseLineHeight: TextUnit,
    textScale: Float,
    allowMultiline: Boolean,
    density: Density,
    minimumHeight: Dp = 56.dp,
): Dp {
    val lineCount = if (allowMultiline) 2 else 1
    val scaledLineHeight = with(density) { (baseLineHeight * textScale).toDp() }
    val fieldHeight = scaledLineHeight * lineCount + (AnswerFieldVerticalPadding * 2)
    return maxOf(fieldHeight, minimumHeight)
}

internal data class GalleryAnswerInputAlignment(
    val nextExpectedIndex: Int,
)

internal fun answerInputAlignment(
    answerInput: String,
    expectedAnswer: String,
): GalleryAnswerInputAlignment {
    var expectedIndex = 0
    var inputIndex = 0

    while (expectedIndex < expectedAnswer.length && inputIndex < answerInput.length) {
        val expectedChar = expectedAnswer[expectedIndex]
        val inputChar = answerInput[inputIndex]

        when {
            expectedChar.isWhitespace() && inputChar.isWhitespace() -> {
                expectedIndex++
                inputIndex++
            }
            expectedChar.isWhitespace() -> {
                expectedIndex++
            }
            inputChar.toString().matchesExpectedSymbol(expectedChar.toString()) -> {
                expectedIndex++
                inputIndex++
            }
            else -> break
        }
    }

    while (expectedIndex < expectedAnswer.length && expectedAnswer[expectedIndex].isWhitespace()) {
        val nextInputChar = answerInput.getOrNull(inputIndex)
        if (nextInputChar?.isWhitespace() == true) break
        expectedIndex++
    }

    return GalleryAnswerInputAlignment(nextExpectedIndex = expectedIndex)
}

internal fun String.matchesExpectedSymbol(expectedSymbol: String): Boolean {
    return lowercase() == expectedSymbol.lowercase()
}
