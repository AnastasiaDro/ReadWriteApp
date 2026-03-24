package com.cerebus.fairy_tales.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.AnswerInputRow
import com.cerebus.core.ui.components.OverlayHelpToggleChip
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType

@Composable
internal fun FairyTalesTopRightHelpChips(
    isHintEnabled: Boolean,
    isShowWordEnabled: Boolean,
    isSimplifiedKeyboardEnabled: Boolean,
    usedHint: Boolean,
    usedShowWord: Boolean,
    usedSimplifiedKeyboard: Boolean,
    onHintToggle: (Boolean) -> Unit,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                checked = isHintEnabled,
                wasUsed = usedHint,
                onClick = { onHintToggle(!isHintEnabled) },
            )
            OverlayHelpToggleChip(
                label = "Word",
                checked = isShowWordEnabled,
                wasUsed = usedShowWord,
                onClick = { onShowWordToggle(!isShowWordEnabled) },
            )
        }
        OverlayHelpToggleChip(
            label = "Aa",
            checked = isSimplifiedKeyboardEnabled,
            wasUsed = usedSimplifiedKeyboard,
            onClick = { onSimplifyKeyboardToggle(!isSimplifiedKeyboardEnabled) },
        )
    }
}

@Composable
internal fun FairyTalesAnswerSection(
    answerInput: String,
    expectedAnswer: String,
    inputFeedbackType: TrainingKeyboardFeedbackType?,
    isHintEnabled: Boolean,
    isShiftEnabled: Boolean,
    isSubmitEnabled: Boolean,
    availableWidth: Dp,
    fieldReferenceWidth: Dp,
    isStacked: Boolean,
    minimumFieldWidth: Dp,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
    alignToStart: Boolean = false,
    onFieldClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val checkButtonWidth = if (isCompact) 48.dp else 56.dp
    val buttonSpacing = if (isCompact) 8.dp else 12.dp
    val maxFieldWidth = (availableWidth - checkButtonWidth - buttonSpacing).coerceAtLeast(minimumFieldWidth)
    val allowMultilineAnswer = if (isCompact) {
        false
    } else {
        expectedAnswer.length > 10 || expectedAnswer.contains(' ')
    }
    val wordCount = expectedAnswer.trim()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }
    val shouldExpandForSingleWord = wordCount == 1
    val baseFieldWidth = if (isStacked) maxFieldWidth else minOf(fieldReferenceWidth, maxFieldWidth)
    val fieldWidth = if (shouldExpandForSingleWord) maxFieldWidth else baseFieldWidth

    Column(
        modifier = modifier.then(
            if (alignToStart || shouldExpandForSingleWord) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
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
            minFieldWidth = baseFieldWidth,
            adaptiveFieldWidth = shouldExpandForSingleWord,
            buttonSize = checkButtonWidth,
            buttonSpacing = buttonSpacing,
            matchFieldHeight = isCompact,
            textScaleOverride = if (isCompact) 1f else null,
            multilineMaxLines = 3,
            revealExpectedAnswer = isHintEnabled,
            isShiftEnabled = isShiftEnabled,
            submitEnabled = isSubmitEnabled,
            onFieldClick = onFieldClick,
            onSubmit = onSubmit,
        )
    }
}

internal data class FairyTalesAnswerInputAlignment(
    val nextExpectedIndex: Int,
)

internal fun fairyTalesAnswerInputAlignment(
    answerInput: String,
    expectedAnswer: String,
): FairyTalesAnswerInputAlignment {
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
            expectedChar.isWhitespace() -> expectedIndex++
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

    return FairyTalesAnswerInputAlignment(nextExpectedIndex = expectedIndex)
}

internal fun String.matchesExpectedSymbol(expectedSymbol: String): Boolean {
    return lowercase() == expectedSymbol.lowercase()
}

internal fun String.canonicalizeOptionalSpacesForExpected(expectedAnswer: String): String {
    val normalizedUser = filterNot(Char::isWhitespace)
    val normalizedExpected = expectedAnswer.filterNot(Char::isWhitespace)
    return if (normalizedUser.equals(normalizedExpected, ignoreCase = true)) {
        expectedAnswer
    } else {
        this
    }
}

internal fun String.extractKeyboardSymbols(): Set<String> {
    return lowercase()
        .filter { it.isLetterOrDigit() }
        .map { it.toString() }
        .toSet()
}
