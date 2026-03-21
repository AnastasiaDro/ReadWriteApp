package com.cerebus.fairy_tales.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cerebus.fairy_tales.presentation.util.rememberPlatformMessenger
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import readwriteapp.feature.fairy_tales.generated.resources.Res
import readwriteapp.feature.fairy_tales.generated.resources.koza

private val demoFairyTales = listOf(
    FairyTaleListItem(
        id = "little-red-riding-hood",
        title = "Идёт коза рогатая",
        description = "За малыми ребятами, ножками - топ-топ, ручками - хлоп-хлоп ...",
        coverColor = Color(0xFFE7A86A),
        coverRes = Res.drawable.koza,
    ),
    FairyTaleListItem(
        id = "three-little-pigs",
        title = "Три поросенка",
        description = "История про домики, ветер и то, как смекалка помогает справиться с бедой.",
        coverColor = Color(0xFFB8D49C),
    ),
    FairyTaleListItem(
        id = "snow-queen",
        title = "Снежная королева",
        description = "Зимняя сказка о дружбе, поиске близкого человека и смелом путешествии.",
        coverColor = Color(0xFF9EC7E8),
    ),
)

@Composable
fun FairyTalesRoute(
    onBackClick: () -> Unit = {},
) {
    val messenger = rememberPlatformMessenger()

    FairyTalesScreen(
        onBackClick = onBackClick,
        onCreateFairyTaleClick = {
            messenger.showMessage("Функционал сказок еще в разработке")
        },
    )
}

@Composable
fun FairyTalesScreen(
    onBackClick: () -> Unit,
    onCreateFairyTaleClick: () -> Unit,
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
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = "Назад")
        }

        Text(
            text = "Сказки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(demoFairyTales) { fairyTale ->
                FairyTaleRow(
                    item = fairyTale,
                    onClick = {},
                )
            }
        }

        Button(
            onClick = onCreateFairyTaleClick,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(text = "Добавить сказку")
        }
    }
}

@Composable
private fun FairyTaleRow(
    item: FairyTaleListItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.coverRes != null) {
            Image(
                painter = painterResource(item.coverRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(item.coverColor),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class FairyTaleListItem(
    val id: String,
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
)
