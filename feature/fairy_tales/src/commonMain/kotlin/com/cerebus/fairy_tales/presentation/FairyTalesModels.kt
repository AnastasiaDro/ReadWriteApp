package com.cerebus.fairy_tales.presentation

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import readwriteapp.feature.fairy_tales.generated.resources.Res
import readwriteapp.feature.fairy_tales.generated.resources.koza

data class FairyTaleContent(
    val id: String,
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val animationAssetPath: String? = null,
    val storyText: String,
)

internal object FairyTalesCatalog {
    val items = listOf(
        FairyTaleContent(
            id = "koza-rogataya",
            title = "Идёт коза рогатая",
            description = "За малыми ребятами, ножками - топ-топ, ручками - хлоп-хлоп ...",
            coverColor = Color(0xFFE7A86A),
            coverRes = Res.drawable.koza,
            animationAssetPath = "files/koza.json",
            storyText = "Здесь будет полный текст сказки и разметка для чтения.",
        ),
        FairyTaleContent(
            id = "three-little-pigs",
            title = "Три поросенка",
            description = "История про домики, ветер и то, как смекалка помогает справиться с бедой.",
            coverColor = Color(0xFFB8D49C),
            storyText = "Здесь будет полный текст сказки про трех поросят.",
        ),
        FairyTaleContent(
            id = "snow-queen",
            title = "Снежная королева",
            description = "Зимняя сказка о дружбе, поиске близкого человека и смелом путешествии.",
            coverColor = Color(0xFF9EC7E8),
            storyText = "Здесь будет полный текст сказки про Снежную королеву.",
        ),
    )

    fun findById(id: String): FairyTaleContent? = items.firstOrNull { it.id == id }
}

data class FairyTalesUiState(
    val fairyTaleId: String,
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val animationAssetPath: String? = null,
    val storyText: String,
)
