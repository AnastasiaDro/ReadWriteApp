package com.cerebus.fairy_tales.presentation

import androidx.compose.ui.graphics.Color
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import org.jetbrains.compose.resources.DrawableResource
import readwriteapp.feature.fairy_tales.generated.resources.Res
import readwriteapp.feature.fairy_tales.generated.resources.koza
import readwriteapp.feature.fairy_tales.generated.resources.horse

internal const val KOZA_FAIRY_TALE_ID = "koza-rogataya"
internal const val KOZA_IDLE_ASSET = "files/koza_idle.json"
internal const val KOZA_WALK_ASSET = "files/1_koza_walk.json"
internal const val KOZA_BODAET_ASSET = "files/6_koza_bodaet.json"
internal const val KOZA_HLOP_ASSET = "files/4_koza_hlop.json"
internal const val KOZA_TOP_TOP_ASSET = "files/3_koza_top_top.json"

sealed interface FairyTaleAnimationKind {
    val assetPath: String?
    val loopDurationMillis: Long?

    data object None : FairyTaleAnimationKind {
        override val assetPath: String? = null
        override val loopDurationMillis: Long? = null
    }

    data object Idle : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_IDLE_ASSET
        override val loopDurationMillis: Long = 3_000L
    }

    data object Walk : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_WALK_ASSET
        override val loopDurationMillis: Long = 3_000L
    }

    data object TopTop : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_TOP_TOP_ASSET
        override val loopDurationMillis: Long = 4_000L
    }

    data object Hlop : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_HLOP_ASSET
        override val loopDurationMillis: Long = 3_000L
    }

    data object Bodaet : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_BODAET_ASSET
        override val loopDurationMillis: Long = 7_500L
    }
}

data class FairyTaleStoryLine(
    val text: String,
    val soundResourcePath: String,
    val soundFileName: String,
    val animationKind: FairyTaleAnimationKind,
)

data class FairyTaleContent(
    val id: String,
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val storyLines: List<FairyTaleStoryLine> = emptyList(),
)

internal object FairyTalesCatalog {
    val items = listOf(
        FairyTaleContent(
            id = KOZA_FAIRY_TALE_ID,
            title = "Идёт коза рогатая",
            description = "Потешка про козу, которую можно читать и печатать по строчкам.",
            coverColor = Color(0xFFE7A86A),
            coverRes = Res.drawable.koza,
            storyLines = listOf(
                FairyTaleStoryLine(
                    text = "Идёт коза рогатая",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "За малыми ребятами",
                    soundResourcePath = "files/koza_story/2_for_kids.m4a",
                    soundFileName = "2_for_kids.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "Ножками топ топ",
                    soundResourcePath = "files/koza_story/3_top-top.m4a",
                    soundFileName = "3_top-top.m4a",
                    animationKind = FairyTaleAnimationKind.TopTop,
                ),
                FairyTaleStoryLine(
                    text = "Глазками хлоп хлоп",
                    soundResourcePath = "files/koza_story/4_eyes_hlop.m4a",
                    soundFileName = "4_eyes_hlop.m4a",
                    animationKind = FairyTaleAnimationKind.Hlop,
                ),
                FairyTaleStoryLine(
                    text = "Кто кашу не ест",
                    soundResourcePath = "files/koza_story/5_porridge.m4a",
                    soundFileName = "5_porridge.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "Молока не пьёт",
                    soundResourcePath = "files/koza_story/6_milk.m4a",
                    soundFileName = "6_milk.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "Того забодает",
                    soundResourcePath = "files/koza_story/7_zabodaet.m4a",
                    soundFileName = "7_zabodaet.m4a",
                    animationKind = FairyTaleAnimationKind.Bodaet,
                ),
            ),
        ),
        FairyTaleContent(
            id = "three-little-pigs",
            title = "Я люблю свою лошадку",
            description = "Причешу ей шёрстку гладко...",
            coverColor = Color(0xFFB8D49C),
            coverRes = Res.drawable.horse,
            storyLines = listOf(
                FairyTaleStoryLine(
                    text = "Я люблю свою лошадку",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "Причешу ей шёрстку гладко",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "Гребешком приглажу хвостик",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
                FairyTaleStoryLine(
                    text = "И верхом поеду в гости",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    animationKind = FairyTaleAnimationKind.Walk,
                ),
            ),
        ),
    )

    fun findById(id: String): FairyTaleContent? = items.firstOrNull { it.id == id }
}

sealed class FairyTaleAnimationState {
    abstract val assetPath: String?

    data object None : FairyTaleAnimationState() {
        override val assetPath: String? = null
    }

    data object Idle : FairyTaleAnimationState() {
        override val assetPath: String = KOZA_IDLE_ASSET
    }

    data object Walk : FairyTaleAnimationState() {
        override val assetPath: String = KOZA_WALK_ASSET
    }

    data class Playback(
        val kind: FairyTaleAnimationKind,
        val playbackToken: Long,
    ) : FairyTaleAnimationState() {
        override val assetPath: String? = kind.assetPath
    }
}

data class FairyTalesUiState(
    val fairyTaleId: String,
    val studentId: String = "",
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val animationState: FairyTaleAnimationState = FairyTaleAnimationState.None,
    val storyLines: List<FairyTaleStoryLine> = emptyList(),
    val currentLineIndex: Int = 0,
    val storyText: String = "",
    val expectedAnswer: String = "",
    val answerInput: String = "",
    val isStoryPlaybackInProgress: Boolean = false,
    val activeSymbols: Set<String> = emptySet(),
    val isShiftEnabled: Boolean = false,
    val hideDigitsOnTightScreen: Boolean = true,
    val keyboardFeedbackKey: String? = null,
    val keyboardFeedbackType: TrainingKeyboardFeedbackType? = null,
    val inputFeedbackType: TrainingKeyboardFeedbackType? = null,
    val isHintVisible: Boolean = true,
    val isInputHintEnabled: Boolean = false,
    val isSimplifiedKeyboardEnabled: Boolean = false,
    val usedHint: Boolean = false,
    val usedShowWord: Boolean = false,
    val usedSimplifiedKeyboard: Boolean = false,
    val feedback: FairyTalesFeedbackUi? = null,
)

data class FairyTalesFeedbackUi(
    val message: String,
    val emoji: String,
)

internal val FairyTalesUiState.currentStoryLine: FairyTaleStoryLine?
    get() = storyLines.getOrNull(currentLineIndex)
