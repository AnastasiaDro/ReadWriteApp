package com.cerebus.fairy_tales.presentation

import androidx.compose.ui.graphics.Color
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import kotlin.math.ceil
import org.jetbrains.compose.resources.DrawableResource
import readwriteapp.feature.fairy_tales.generated.resources.Res
import readwriteapp.feature.fairy_tales.generated.resources.koza

internal const val KOZA_FAIRY_TALE_ID = "koza-rogataya"
internal const val KOZA_IDLE_ASSET = "files/koza_idle.json"
internal const val KOZA_BODAET_ASSET = "files/6_koza_bodaet.json"
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

    data object TopTop : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_TOP_TOP_ASSET
        override val loopDurationMillis: Long = 4_000L
    }

    data object Bodaet : FairyTaleAnimationKind {
        override val assetPath: String = KOZA_BODAET_ASSET
        override val loopDurationMillis: Long = 7_500L
    }
}

data class FairyTalePlaybackPlan(
    val iterations: Int,
    val totalDurationMillis: Long,
)

internal fun FairyTaleAnimationKind.planForAudio(audioDurationMillis: Long?): FairyTalePlaybackPlan {
    val loopDuration = loopDurationMillis
    if (assetPath.isNullOrBlank() || loopDuration == null || loopDuration <= 0L) {
        return FairyTalePlaybackPlan(
            iterations = 1,
            totalDurationMillis = (audioDurationMillis ?: 0L).coerceAtLeast(0L),
        )
    }
    val safeAudioDuration = (audioDurationMillis ?: loopDuration).coerceAtLeast(1L)
    val iterations = ceil(safeAudioDuration.toDouble() / loopDuration.toDouble()).toInt().coerceAtLeast(1)
    return FairyTalePlaybackPlan(
        iterations = iterations,
        totalDurationMillis = iterations * loopDuration,
    )
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
                    animationKind = FairyTaleAnimationKind.Idle,
                ),
                FairyTaleStoryLine(
                    text = "За малыми ребятами",
                    soundResourcePath = "files/koza_story/2_for_kids.m4a",
                    soundFileName = "2_for_kids.m4a",
                    animationKind = FairyTaleAnimationKind.Idle,
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
                    animationKind = FairyTaleAnimationKind.Idle,
                ),
                FairyTaleStoryLine(
                    text = "Кто кашу не ест",
                    soundResourcePath = "files/koza_story/5_porridge.m4a",
                    soundFileName = "5_porridge.m4a",
                    animationKind = FairyTaleAnimationKind.Idle,
                ),
                FairyTaleStoryLine(
                    text = "Молока не пьёт",
                    soundResourcePath = "files/koza_story/6_milk.m4a",
                    soundFileName = "6_milk.m4a",
                    animationKind = FairyTaleAnimationKind.Idle,
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
            title = "Три поросенка",
            description = "История про домики, ветер и то, как смекалка помогает справиться с бедой.",
            coverColor = Color(0xFFB8D49C),
        ),
        FairyTaleContent(
            id = "snow-queen",
            title = "Снежная королева",
            description = "Зимняя сказка о дружбе, поиске близкого человека и смелом путешествии.",
            coverColor = Color(0xFF9EC7E8),
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

    data class Playback(
        val kind: FairyTaleAnimationKind,
        val playbackToken: Long,
        val iterations: Int,
        val totalDurationMillis: Long,
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
