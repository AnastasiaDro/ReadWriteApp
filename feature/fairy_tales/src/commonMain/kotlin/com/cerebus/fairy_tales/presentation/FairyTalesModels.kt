package com.cerebus.fairy_tales.presentation

import androidx.compose.ui.graphics.Color
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import org.jetbrains.compose.resources.DrawableResource
import readwriteapp.feature.fairy_tales.generated.resources.Res
import readwriteapp.feature.fairy_tales.generated.resources._1_love_my_horse
import readwriteapp.feature.fairy_tales.generated.resources._2_fure
import readwriteapp.feature.fairy_tales.generated.resources._3_tail
import readwriteapp.feature.fairy_tales.generated.resources._4_riding
import readwriteapp.feature.fairy_tales.generated.resources.horse
import readwriteapp.feature.fairy_tales.generated.resources.koza

/** Goat block **/
internal const val KOZA_FAIRY_TALE_ID = "koza-rogataya"
internal const val KOZA_IDLE_ASSET = "files/koza_idle.json"
internal const val KOZA_WALK_ASSET = "files/1_koza_walk.json"
internal const val KOZA_BODAET_ASSET = "files/6_koza_bodaet.json"
internal const val KOZA_HLOP_ASSET = "files/4_koza_hlop.json"
internal const val KOZA_TOP_TOP_ASSET = "files/3_koza_top_top.json"

/** Horse block **/
internal const val HORSE_TALE_ID = "love-my-horse"

sealed interface FairyTaleVisualContent {
    data object None : FairyTaleVisualContent

    data class Lottie(
        val assetPath: String,
        val loopDurationMillis: Long? = null,
    ) : FairyTaleVisualContent

    data class Image(
        val resource: DrawableResource,
    ) : FairyTaleVisualContent
}

data class FairyTaleVisualScheme(
    val initial: FairyTaleVisualContent = FairyTaleVisualContent.None,
    val betweenLines: FairyTaleVisualContent = FairyTaleVisualContent.None,
    val completed: FairyTaleVisualContent = FairyTaleVisualContent.None,
)

data class FairyTaleStoryLine(
    val text: String,
    val soundResourcePath: String,
    val soundFileName: String,
    val playbackVisual: FairyTaleVisualContent,
)

data class FairyTaleContent(
    val id: String,
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val visualScheme: FairyTaleVisualScheme = FairyTaleVisualScheme(),
    val storyLines: List<FairyTaleStoryLine> = emptyList(),
)

internal object FairyTalesCatalog {
    private val goatIdleVisual = FairyTaleVisualContent.Lottie(
        assetPath = KOZA_IDLE_ASSET,
        loopDurationMillis = 3_000L,
    )
    private val goatWalkVisual = FairyTaleVisualContent.Lottie(
        assetPath = KOZA_WALK_ASSET,
        loopDurationMillis = 3_000L,
    )
    private val goatTopTopVisual = FairyTaleVisualContent.Lottie(
        assetPath = KOZA_TOP_TOP_ASSET,
        loopDurationMillis = 4_000L,
    )
    private val goatHlopVisual = FairyTaleVisualContent.Lottie(
        assetPath = KOZA_HLOP_ASSET,
        loopDurationMillis = 3_000L,
    )
    private val goatBodaetVisual = FairyTaleVisualContent.Lottie(
        assetPath = KOZA_BODAET_ASSET,
        loopDurationMillis = 7_500L,
    )
    private val horseStillVisual = FairyTaleVisualContent.Image(Res.drawable.horse)
    private val horseLoveVisual = FairyTaleVisualContent.Image(Res.drawable._1_love_my_horse)
    private val horseFureVisual = FairyTaleVisualContent.Image(Res.drawable._2_fure)
    private val horseTailVisual = FairyTaleVisualContent.Image(Res.drawable._3_tail)
    private val horseRidingVisual = FairyTaleVisualContent.Image(Res.drawable._4_riding)

    val items = listOf(
        FairyTaleContent(
            id = KOZA_FAIRY_TALE_ID,
            title = "Идёт коза рогатая",
            description = "Потешка про козу, которую можно читать и печатать по строчкам.",
            coverColor = Color(0xFFE7A86A),
            coverRes = Res.drawable.koza,
            visualScheme = FairyTaleVisualScheme(
                initial = goatIdleVisual,
                betweenLines = goatWalkVisual,
                completed = goatIdleVisual,
            ),
            storyLines = listOf(
                FairyTaleStoryLine(
                    text = "Идёт коза рогатая",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    playbackVisual = goatWalkVisual,
                ),
                FairyTaleStoryLine(
                    text = "За малыми ребятами",
                    soundResourcePath = "files/koza_story/2_for_kids.m4a",
                    soundFileName = "2_for_kids.m4a",
                    playbackVisual = goatWalkVisual,
                ),
                FairyTaleStoryLine(
                    text = "Ножками топ топ",
                    soundResourcePath = "files/koza_story/3_top-top.m4a",
                    soundFileName = "3_top-top.m4a",
                    playbackVisual = goatTopTopVisual,
                ),
                FairyTaleStoryLine(
                    text = "Глазками хлоп хлоп",
                    soundResourcePath = "files/koza_story/4_eyes_hlop.m4a",
                    soundFileName = "4_eyes_hlop.m4a",
                    playbackVisual = goatHlopVisual,
                ),
                FairyTaleStoryLine(
                    text = "Кто кашу не ест",
                    soundResourcePath = "files/koza_story/5_porridge.m4a",
                    soundFileName = "5_porridge.m4a",
                    playbackVisual = goatWalkVisual,
                ),
                FairyTaleStoryLine(
                    text = "Молока не пьёт",
                    soundResourcePath = "files/koza_story/6_milk.m4a",
                    soundFileName = "6_milk.m4a",
                    playbackVisual = goatWalkVisual,
                ),
                FairyTaleStoryLine(
                    text = "Того забодает",
                    soundResourcePath = "files/koza_story/7_zabodaet.m4a",
                    soundFileName = "7_zabodaet.m4a",
                    playbackVisual = goatBodaetVisual,
                ),
            ),
        ),
        FairyTaleContent(
            id = HORSE_TALE_ID,
            title = "Я люблю свою лошадку",
            description = "Стихотворение Агнии Барто про лошадку и заботу о ней",
            coverColor = Color(0xFFB8D49C),
            coverRes = Res.drawable.horse,
            visualScheme = FairyTaleVisualScheme(
                initial = horseLoveVisual,
                betweenLines = FairyTaleVisualContent.None,
                completed = horseLoveVisual,
            ),
            storyLines = listOf(
                FairyTaleStoryLine(
                    text = "Я люблю свою лошадку",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    playbackVisual = horseFureVisual,
                ),
                FairyTaleStoryLine(
                    text = "Причешу ей шёрстку гладко",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    playbackVisual = horseTailVisual,
                ),
                FairyTaleStoryLine(
                    text = "Гребешком приглажу хвостик",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    playbackVisual = horseRidingVisual,
                ),
                FairyTaleStoryLine(
                    text = "И верхом поеду в гости",
                    soundResourcePath = "files/koza_story/1_walk.m4a",
                    soundFileName = "1_walk.m4a",
                    playbackVisual = horseRidingVisual,
                ),
            ),
        ),
    )

    fun findById(id: String): FairyTaleContent? = items.firstOrNull { it.id == id }
}

sealed class FairyTaleVisualState {
    abstract val content: FairyTaleVisualContent

    data class Waiting(
        override val content: FairyTaleVisualContent,
    ) : FairyTaleVisualState()

    data class Playback(
        override val content: FairyTaleVisualContent,
        val playbackToken: Long,
    ) : FairyTaleVisualState()
}

data class FairyTalesUiState(
    val fairyTaleId: String,
    val studentId: String = "",
    val title: String,
    val description: String,
    val coverColor: Color,
    val coverRes: DrawableResource? = null,
    val visualScheme: FairyTaleVisualScheme = FairyTaleVisualScheme(),
    val visualState: FairyTaleVisualState = FairyTaleVisualState.Waiting(FairyTaleVisualContent.None),
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

internal val FairyTaleVisualContent.lottieAssetPath: String?
    get() = (this as? FairyTaleVisualContent.Lottie)?.assetPath

internal val FairyTaleVisualContent.loopDurationMillis: Long?
    get() = (this as? FairyTaleVisualContent.Lottie)?.loopDurationMillis

internal val FairyTaleVisualContent.visualKey: String
    get() = when (this) {
        FairyTaleVisualContent.None -> "none"
        is FairyTaleVisualContent.Image -> "image:${resource::class.qualifiedName}:${resource.hashCode()}"
        is FairyTaleVisualContent.Lottie -> "lottie:$assetPath"
    }

internal val FairyTaleVisualState.renderKey: String
    get() = when (this) {
        is FairyTaleVisualState.Playback -> "playback:$playbackToken:${content.visualKey}"
        is FairyTaleVisualState.Waiting -> "waiting:${content.visualKey}"
    }
