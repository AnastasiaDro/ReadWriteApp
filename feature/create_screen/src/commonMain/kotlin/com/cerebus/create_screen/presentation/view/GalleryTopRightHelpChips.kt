package com.cerebus.create_screen.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.OverlayHelpToggleChip

@Composable
internal fun GalleryTopRightHelpChips(
    isShowWordEnabled: Boolean,
    isSimplifiedKeyboardEnabled: Boolean,
    usedShowWord: Boolean,
    usedSimplifiedKeyboard: Boolean,
    onShowWordToggle: (Boolean) -> Unit,
    onSimplifyKeyboardToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        OverlayHelpToggleChip(
            label = "Word",
            checked = isShowWordEnabled,
            wasUsed = usedShowWord,
            onClick = { onShowWordToggle(!isShowWordEnabled) },
        )
        OverlayHelpToggleChip(
            label = "Aa",
            checked = isSimplifiedKeyboardEnabled,
            wasUsed = usedSimplifiedKeyboard,
            onClick = { onSimplifyKeyboardToggle(!isSimplifiedKeyboardEnabled) },
        )
    }
}
