package com.cerebus.create_screen.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.cerebus.data.flashcards.domain.models.Flashcard
import kotlin.math.absoluteValue

@Composable
internal fun GalleryTrainingCard(
    cards: List<Flashcard>,
    currentIndex: Int,
    animatedScrollTargetIndex: Int?,
    onAnimatedScrollTargetConsumed: () -> Unit,
    isHintVisible: Boolean,
    isLandscape: Boolean,
    isPhoneLandscape: Boolean,
    preferredCardSize: Dp? = null,
    modifier: Modifier = Modifier,
    onCardSelected: (Int) -> Unit,
) {
    if (cards.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceIn(0, cards.lastIndex),
        pageCount = { cards.size },
    )

    LaunchedEffect(currentIndex, cards.size) {
        val targetPage = currentIndex.coerceIn(0, cards.lastIndex)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(animatedScrollTargetIndex, cards.size) {
        val targetPage = animatedScrollTargetIndex ?: return@LaunchedEffect
        val safeTargetPage = targetPage.coerceIn(0, cards.lastIndex)
        if (pagerState.currentPage != safeTargetPage) {
            pagerState.animateScrollToPage(safeTargetPage)
        }
        onAnimatedScrollTargetConsumed()
    }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != currentIndex) {
            onCardSelected(pagerState.settledPage)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val pageWidth = preferredCardSize ?: when {
            isLandscape -> minOf(360.dp, (maxWidth - 8.dp).coerceAtLeast(120.dp))
            else -> minOf(360.dp, (maxWidth * 0.74f).coerceAtLeast(120.dp))
        }
        val horizontalPeek = ((maxWidth - pageWidth) / 2f).coerceAtLeast(0.dp)
        val pageSpacing = when {
            isPhoneLandscape -> 6.dp
            isLandscape -> 10.dp
            else -> 8.dp
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = horizontalPeek),
            pageSpacing = pageSpacing,
            pageSize = PageSize.Fixed(pageWidth),
            key = { index -> cards[index].id },
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val coercedOffset = pageOffset.coerceIn(0f, 1f)
            val scale = if (isLandscape) {
                1f - (0.5f * coercedOffset)
            } else {
                1f - (0.18f * coercedOffset)
            }
            val alpha = if (isLandscape) {
                1f - (0.28f * coercedOffset)
            } else if (page == pagerState.currentPage) {
                1f
            } else {
                0.72f
            }

            GalleryMainCard(
                card = cards[page],
                isHintVisible = isHintVisible && page == currentIndex,
                isPhoneLandscape = isPhoneLandscape,
                cardSize = pageWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                    },
                alpha = alpha,
                onClick = {
                    if (page != currentIndex) {
                        onCardSelected(page)
                    }
                },
            )
        }
    }
}

@Composable
private fun GalleryMainCard(
    card: Flashcard,
    isHintVisible: Boolean,
    isPhoneLandscape: Boolean,
    cardSize: Dp,
    modifier: Modifier = Modifier,
    alpha: Float,
    onClick: () -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext).build()
    }
    var imageLoadFailed by remember(card.imageUrl) { mutableStateOf(false) }
    val normalizedImagePath = card.imageUrl.trim()
    val isTextCard = normalizedImagePath.isBlank() || imageLoadFailed

    Box(
        modifier = modifier
            .alpha(alpha)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
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
        }

        GalleryHint(
            text = card.name.uppercase(),
            visible = isHintVisible && !isTextCard,
            modifier = if (isPhoneLandscape) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 10.dp)
                    .offset(x = (-12).dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            },
        )
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
