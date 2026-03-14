package com.cerebus.customkeyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val englishRows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "'"),
    listOf("shift", "?", "z", "x", "c", "v", "b", "n", "m", "⌫"),
    listOf("abc", ",", "space", ".", "OK"),
)

private val russianRows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"),
    listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
    listOf("shift", "я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "⌫"),
    listOf("settings", "ъ", "ё", "space", ".", "OK"),
)

private val rowWeights = listOf(0.17f, 0.22f, 0.22f, 0.22f, 0.17f)
private val keyShape = RoundedCornerShape(8.dp)
private val keyBackgroundColor = Color(0xFFDADADA)
private val keyTextColor = Color(0xFF000000)
private val actionTextColor = Color(0xFF818285)
private val inactiveKeyBackgroundColor = Color(0xFF818285)
private val inactiveKeyTextColor = Color(0xFFB0B0B3)
private val shiftAccentColor = Color(0xFF8000FF)
private val keyboardContainerShape = RoundedCornerShape(18.dp)

@Composable
fun TrainingKeyboard(
    referenceText: String,
    activeSymbols: Set<String>,
    isShiftEnabled: Boolean,
    onShiftChanged: (Boolean) -> Unit,
    onSymbolPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onSpacePressed: () -> Unit,
    onSubmitPressed: () -> Unit,
    onSettingsPressed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isLandscape = windowWidthDp > windowHeightDp
    val keyboardHeight = if (isLandscape) {
        windowHeightDp * 0.5f
    } else {
        windowHeightDp * 0.3f
    }.coerceIn(220.dp, 340.dp)

    val rows = remember(referenceText) { resolveKeyboardRows(referenceText = referenceText) }
    val normalizedActiveSymbols = remember(activeSymbols) {
        activeSymbols.mapTo(mutableSetOf()) { it.lowercase() }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF818285),
        shape = keyboardContainerShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .height(keyboardHeight)
                .padding(start = 4.dp, end = 4.dp, top = 3.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(rowWeights.getOrElse(rowIndex) { 0.2f }),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { key ->
                        val keySpec = resolveKeySpec(key)
                        val isKeyEnabled = isKeyEnabled(
                            key = key,
                            keySpec = keySpec,
                            normalizedActiveSymbols = normalizedActiveSymbols,
                        )
                        Box(
                            modifier = Modifier
                                .weight(keySpec.weight)
                                .fillMaxHeight()
                                .padding(horizontal = 1.5.dp, vertical = 4.dp),
                        ) {
                            KeyboardKey(
                                label = when {
                                    key == "space" -> ""
                                    else -> keySpec.visibleLabel(isShiftEnabled)
                                },
                                onClick = {
                                    when (key) {
                                        "shift", "abc" -> onShiftChanged(!isShiftEnabled)
                                        "settings" -> onSettingsPressed()
                                        "space" -> onSpacePressed()
                                        "⌫" -> onBackspacePressed()
                                        "OK" -> onSubmitPressed()
                                        else -> onSymbolPressed(keySpec.emittedValue(isShiftEnabled))
                                    }
                                },
                                isAction = keySpec.isAction,
                                icon = keySpec.icon,
                                isShiftActive = key == "shift" && isShiftEnabled,
                                isEnabled = isKeyEnabled,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveKeyboardRows(
    referenceText: String,
): List<List<String>> {
    return if (containsCyrillic(referenceText)) russianRows else englishRows
}

private fun containsCyrillic(text: String): Boolean {
    return text.any { char -> char.lowercaseChar() in 'а'..'я' || char.lowercaseChar() == 'ё' }
}

private data class KeySpec(
    val weight: Float = 1f,
    val isAction: Boolean = false,
    val icon: ImageVector? = null,
    val visibleLabel: (Boolean) -> String,
    val emittedValue: (Boolean) -> String = { "" },
)

private fun resolveKeySpec(key: String): KeySpec {
    return when (key) {
        "shift" -> KeySpec(
            weight = 1f,
            isAction = true,
            icon = Icons.Filled.KeyboardArrowUp,
            visibleLabel = { "" },
        )
        "⌫" -> KeySpec(
            weight = 1f,
            isAction = true,
            icon = Icons.AutoMirrored.Filled.Backspace,
            visibleLabel = { "" },
        )
        "space" -> KeySpec(
            weight = 5.2f,
            isAction = true,
            visibleLabel = { "" },
        )
        "abc" -> KeySpec(
            weight = 1.2f,
            isAction = true,
            visibleLabel = { shifted -> if (shifted) "ABC" else "abc" },
        )
        "settings" -> KeySpec(
            weight = 1.1f,
            isAction = true,
            icon = Icons.Filled.Settings,
            visibleLabel = { "" },
        )
        "OK" -> KeySpec(
            weight = 1.2f,
            isAction = true,
            visibleLabel = { "OK" },
        )
        else -> KeySpec(
            visibleLabel = { shifted -> if (shifted) key.uppercase() else key.lowercase() },
            emittedValue = { shifted -> if (shifted) key.uppercase() else key.lowercase() },
        )
    }
}

private fun isKeyEnabled(
    key: String,
    keySpec: KeySpec,
    normalizedActiveSymbols: Set<String>,
): Boolean {
    if (keySpec.isAction) return true
    if (normalizedActiveSymbols.isEmpty()) return true
    return when {
        key.length == 1 && key.first().isLetterOrDigit() -> key.lowercase() in normalizedActiveSymbols
        else -> true
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    isAction: Boolean,
    icon: ImageVector?,
    isShiftActive: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isLandscape = windowWidthDp > windowHeightDp
    val backgroundColor = if (isEnabled) keyBackgroundColor else inactiveKeyBackgroundColor
    val textColor = when {
        !isEnabled -> inactiveKeyTextColor
        isShiftActive -> shiftAccentColor
        isAction -> actionTextColor
        else -> keyTextColor
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(keyShape)
            .background(backgroundColor)
            .clickable(
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val keyHeight = maxHeight

        if (label.isNotEmpty()) {
            val dynamicFontSize = if (!isAction && label.length <= 1) {
                with(density) { (keyHeight * if (isLandscape) 0.6f else 0.5f).toSp() }
            } else {
                with(density) { (keyHeight * 0.34f).toSp() }
            }

            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dynamicFontSize,
                    fontWeight = FontWeight.Normal,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 0.dp),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.height(keyHeight * 0.52f),
            )
        }
    }
}
