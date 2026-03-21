package com.cerebus.game_screen.presentation.view.active_game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage

//TODO раскидать файлы в отдельный пакет
@Composable
fun GameCard(
    cardSize: Dp,
    imagePath: String,
    answer: String,
    isHintVisible: Boolean,
    imageLoader: ImageLoader,
) {
    var imageLoadFailed by remember(imagePath) { mutableStateOf(false) }
    val normalizedImagePath = imagePath.trim()
    val isTextCard = normalizedImagePath.isBlank() || imageLoadFailed

    Box(
        modifier = Modifier
            .size(cardSize)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (isTextCard) {
            CardTextFallback(
                text = answer,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = normalizedImagePath,
                contentDescription = "Game card image",
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { imageLoadFailed = false },
                onError = { imageLoadFailed = true },
            )
        }
        Hint(
            text = answer.uppercase(),
            visible = isHintVisible && !isTextCard,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )
    }
}