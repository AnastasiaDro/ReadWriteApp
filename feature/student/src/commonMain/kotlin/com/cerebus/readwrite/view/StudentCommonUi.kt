package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import org.jetbrains.compose.resources.stringResource
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.create_student_avatar_placeholder

@Composable
fun StudentAvatar(
    avatarUri: String?,
    size: Dp,
) {
    val imageLoader = ImageLoader.Builder(LocalPlatformContext.current).build()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUri.isNullOrBlank()) {
            Text(
                text = stringResource(Res.string.create_student_avatar_placeholder),
                textAlign = TextAlign.Center,
                style = if (size >= 120.dp) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineLarge
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = avatarUri,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
