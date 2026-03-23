package com.cerebus.fairy_tales.presentation

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import readwriteapp.feature.fairy_tales.generated.resources.Res
import readwriteapp.feature.fairy_tales.generated.resources.koza

internal const val KOZA_FAIRY_TALE_ID = "koza-rogataya"
internal const val KOZA_IDLE_ASSET = "files/koza_idle.json"
internal const val KOZA_BODAET_ASSET = "files/koza_bodaet.json"
internal const val KOZA_TOP_TOP_ASSET = "files/koza_top_top.json"
internal const val KOZA_TOP_TOP_SOUND_ASSET = "files/top-top.m4a"

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
            id = KOZA_FAIRY_TALE_ID,
            title = "Идёт коза рогатая",
            description = "За малыми ребятами, ножками - топ-топ, ручками - хлоп-хлоп ...",
            coverColor = Color(0xFFE7A86A),
            coverRes = Res.drawable.koza,
            animationAssetPath = "files/koza_top_top_old.json",
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

sealed class FairyTaleAnimationState {
    open val playbackToken: Long? = null
    open val durationMillis: Long? = null
    abstract val assetPath: String?

    data object None : FairyTaleAnimationState() {
        override val assetPath: String? = null
    }

    data object Idle : FairyTaleAnimationState() {
        override val assetPath: String = KOZA_IDLE_ASSET
    }

    data class Bodaet(
        override val playbackToken: Long,
    ) : FairyTaleAnimationState() {
        override val assetPath: String = KOZA_BODAET_ASSET
        override val durationMillis: Long = 7_500L
    }

    data class TopTop(
        override val playbackToken: Long,
    ) : FairyTaleAnimationState() {
        override val assetPath: String = KOZA_TOP_TOP_ASSET
        override val durationMillis: Long = 4_000L
    }
}

data class FairyTalesUiState(
    val fairyTaleId: String,
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val animationAssetPath: String? = null,
    val animationState: FairyTaleAnimationState = FairyTaleAnimationState.None,
    val storyText: String,
)
