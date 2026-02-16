package com.cerebus.create_screen.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CreateScreenStyles {
    @Composable
    fun deckCreatedTitle(windowWidthDp: Dp): TextStyle {
        val base = MaterialTheme.typography.headlineSmall
        val adaptiveSize = when {
            windowWidthDp >= 840.dp -> 42.sp
            windowWidthDp >= 600.dp -> 34.sp
            else -> 28.sp
        }
        return base.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = adaptiveSize,
            lineHeight = adaptiveSize * 1.2f,
        )
    }
}
