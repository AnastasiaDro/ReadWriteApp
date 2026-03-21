package com.cerebus.create_screen.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.cerebus.data.flashcards.domain.models.Flashcard

@Composable
internal fun GalleryTrainingCard(
    card: Flashcard,
    isHintVisible: Boolean,
    modifier: Modifier = Modifier,
    onSwipePrevious: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext).build()
    }
    var imageLoadFailed by remember(card.imageUrl) { mutableStateOf(false) }
    val normalizedImagePath = card.imageUrl.trim()
    val isTextCard = normalizedImagePath.isBlank() || imageLoadFailed

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val cardSize = if (maxWidth < 360.dp) maxWidth else 360.dp
        var dragAccumulation by remember(card.id) { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .align(Alignment.Center)
                .pointerInput(card.id, onSwipePrevious, onSwipeNext) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulation += dragAmount
                        },
                        onDragEnd = {
                            when {
                                dragAccumulation <= -56f -> onSwipeNext?.invoke()
                                dragAccumulation >= 56f -> onSwipePrevious?.invoke()
                            }
                            dragAccumulation = 0f
                        },
                        onDragCancel = {
                            dragAccumulation = 0f
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .size(cardSize)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (isTextCard) {
                    GalleryCardTextFallback(
                        text = card.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = normalizedImagePath,
                        contentDescription = null,
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { imageLoadFailed = false },
                        onError = { imageLoadFailed = true },
                    )
                }

                GalleryHint(
                    text = card.name.uppercase(),
                    visible = isHintVisible && !isTextCard,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun GalleryCardTextFallback(
    text: String,
    modifier: Modifier = Modifier,
) {
    val normalizedText = text.trim().ifBlank { "?" }
    val textStyle = when {
        normalizedText.length <= 2 -> MaterialTheme.typography.displayLarge
        normalizedText.length <= 6 -> MaterialTheme.typography.displayMedium
        normalizedText.length <= 12 -> MaterialTheme.typography.displaySmall
        else -> MaterialTheme.typography.headlineLarge
    }

    Text(
        text = normalizedText,
        style = textStyle,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun GalleryHint(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun GalleryCardThumbnail(
    card: Flashcard,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext).build()
    }
    val imagePath = card.imageUrl.trim()

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath.isBlank()) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
