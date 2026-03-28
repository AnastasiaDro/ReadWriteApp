package com.cerebus.game_screen.presentation.view.active_game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.HintSize

@Composable
fun Hint(
    text: String,
    visible: Boolean,
    size: HintSize,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val baseStyle = MaterialTheme.typography.titleMedium
    val textScale = when (size) {
        HintSize.SMALL -> 1.2f
        HintSize.MEDIUM -> 2f
        HintSize.LARGE -> 2.5f
    }

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 16.dp),
        style = baseStyle.copy(
            fontSize = baseStyle.fontSize * textScale,
        ),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}
