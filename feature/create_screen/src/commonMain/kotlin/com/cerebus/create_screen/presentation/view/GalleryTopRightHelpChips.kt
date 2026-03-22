package com.cerebus.create_screen.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.OverlayHelpToggleChip

@Composable
internal fun GalleryTopRightHelpChips(
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
