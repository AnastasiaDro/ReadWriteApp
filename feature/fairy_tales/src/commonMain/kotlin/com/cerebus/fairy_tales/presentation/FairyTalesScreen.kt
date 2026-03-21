package com.cerebus.fairy_tales.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.fairy_tales.generated.resources.Res

@Composable
fun FairyTalesScreenRoute(
    fairyTaleId: String,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<FairyTalesViewModel>(
        parameters = { parametersOf(fairyTaleId) },
    )
    val state by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { viewModel.state } }

    FairyTalesScreen(
        state = state,
        onBackClick = onBackClick,
    )
}

@Composable
fun FairyTalesScreen(
    state: FairyTalesUiState,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp,
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = "Назад")
        }

        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        FairyTaleAnimationSlot(
            animationAssetPath = state.animationAssetPath,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text = state.storyText,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun FairyTaleAnimationSlot(
    animationAssetPath: String?,
    modifier: Modifier = Modifier,
) {
    val compositionResult = animationAssetPath?.let { assetPath ->
        rememberLottieComposition(assetPath) {
            val json = Res.readBytes(assetPath).decodeToString()
            LottieCompositionSpec.JsonString(json)
        }
    }
    val composition by (compositionResult ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(null)
    })

    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            animationAssetPath.isNullOrBlank() -> {
                Text(
                    text = "Здесь будет Lottie-анимация",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            composition != null -> {
                Image(
                    painter = rememberLottiePainter(
                        composition = composition,
                        iterations = Compottie.IterateForever,
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            compositionResult?.isFailure == true -> {
                Text(
                    text = "Не удалось загрузить анимацию",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                Text(
                    text = "Загружаем анимацию...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
