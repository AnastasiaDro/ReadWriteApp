package com.cerebus.create_screen.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun GalleryGlobalNavigationControls(
    onPreviousClick: (() -> Unit)?,
    onNextClick: (() -> Unit)?,
    previousLabel: String,
    nextLabel: String,
    isLandscape: Boolean,
    horizontalShift: Dp,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = if (isLandscape) Alignment.Center else Alignment.CenterStart,
        ) {
            GallerySideArrowButton(
                symbol = "‹",
                label = previousLabel,
                enabled = onPreviousClick != null,
                onClick = { onPreviousClick?.invoke() },
                modifier = Modifier.offset(x = -horizontalShift),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = if (isLandscape) Alignment.Center else Alignment.CenterEnd,
        ) {
            GallerySideArrowButton(
                symbol = "›",
                label = nextLabel,
                enabled = onNextClick != null,
                onClick = { onNextClick?.invoke() },
                modifier = Modifier.offset(x = horizontalShift),
            )
        }
    }
}

@Composable
private fun GallerySideArrowButton(
    symbol: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "gallery_side_arrow_scale",
    )

    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(999.dp),
        color = if (!enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else if (isPressed) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (!enabled) {
            0.dp
        } else if (isPressed) {
            4.dp
        } else {
            2.dp
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (!enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                } else if (isPressed) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
