package com.cerebus.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AnswerFieldHorizontalPadding = 16.dp
val AnswerFieldVerticalPadding = 8.dp

@Composable
fun ReadOnlyAnswerField(
    value: String,
    expectedAnswer: String,
    feedbackBorderColor: Color,
    allowMultiline: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val windowHeightDp = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val isTablet = minOf(windowWidthDp, windowHeightDp) >= 600.dp
    val baseTextStyle = MaterialTheme.typography.bodyLarge
    val answerTextScale = if (isTablet) 2f else 1.5f
    val answerTextStyle = baseTextStyle.copy(
        fontSize = baseTextStyle.fontSize * answerTextScale,
        lineHeight = baseTextStyle.lineHeight * answerTextScale,
    )
    val currentSlotBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
    val currentSlotTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    val displayedValue = remember(
        value,
        expectedAnswer,
        currentSlotBackgroundColor,
        currentSlotTextColor,
    ) {
        buildAnswerProgressMask(
            answerInput = value,
            expectedAnswer = expectedAnswer,
            currentSlotBackgroundColor = currentSlotBackgroundColor,
            currentSlotTextColor = currentSlotTextColor,
        )
    }
    val resolvedFeedbackBorderColor = if (feedbackBorderColor == Color.Unspecified) {
        MaterialTheme.colorScheme.outline
    } else {
        feedbackBorderColor
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = resolvedFeedbackBorderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = displayedValue,
            style = answerTextStyle,
            maxLines = if (allowMultiline) 2 else 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AnswerFieldHorizontalPadding,
                    vertical = AnswerFieldVerticalPadding,
                )
                .wrapContentHeight(
                    align = if (allowMultiline) Alignment.Top else Alignment.CenterVertically
                ),
        )
    }
}

private fun buildAnswerProgressMask(
    answerInput: String,
    expectedAnswer: String,
    currentSlotBackgroundColor: Color,
    currentSlotTextColor: Color,
): AnnotatedString {
    if (expectedAnswer.isEmpty()) return AnnotatedString(answerInput)
    return buildAnnotatedString {
        val currentSlotIndex = nextVisibleSlotIndex(
            answerInput = answerInput,
            expectedAnswer = expectedAnswer,
        )
        var inputIndex = 0
        expectedAnswer.forEachIndexed { index, expectedChar ->
            if (index > 0) append(' ')
            val displayedChar = when {
                expectedChar.isWhitespace() && answerInput.getOrNull(inputIndex)?.isWhitespace() == true -> {
                    inputIndex++
                    ' '
                }
                expectedChar.isWhitespace() -> ' '
                inputIndex < answerInput.length -> answerInput[inputIndex++]
                else -> '_'
            }
            val isCurrentSlot = index == currentSlotIndex

            if (isCurrentSlot) {
                pushStyle(
                    SpanStyle(
                        background = currentSlotBackgroundColor,
                        color = currentSlotTextColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                )
                append(displayedChar)
                pop()
            } else {
                append(displayedChar)
            }
        }
    }
}

private fun nextVisibleSlotIndex(
    answerInput: String,
    expectedAnswer: String,
): Int {
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
            else -> {
                expectedIndex++
                inputIndex++
            }
        }
    }

    while (expectedIndex < expectedAnswer.length && expectedAnswer[expectedIndex].isWhitespace()) {
        expectedIndex++
    }

    return expectedIndex.takeIf { it in expectedAnswer.indices } ?: -1
}
