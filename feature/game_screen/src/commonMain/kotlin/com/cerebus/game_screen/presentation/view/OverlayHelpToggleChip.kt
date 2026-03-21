package com.cerebus.game_screen.presentation.view

import androidx.compose.runtime.Composable
import com.cerebus.core.ui.components.OverlayHelpToggleChip as CoreOverlayHelpToggleChip

@Composable
fun OverlayHelpToggleChip(
    label: String,
    checked: Boolean,
    wasUsed: Boolean,
    onClick: () -> Unit,
) {
    CoreOverlayHelpToggleChip(
        label = label,
        checked = checked,
        wasUsed = wasUsed,
        onClick = onClick,
    )
}
