package com.cerebus.fairy_tales.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FairyTalesViewModel(
    fairyTaleId: String,
) : ViewModel() {

    var state by mutableStateOf(
        FairyTalesCatalog.findById(fairyTaleId)?.toUiState()
            ?: FairyTalesCatalog.items.first().toUiState()
    )
        private set

    private val _effects = MutableSharedFlow<FairyTalesEffect>(
        extraBufferCapacity = 1,
    )
    val effects: SharedFlow<FairyTalesEffect> = _effects.asSharedFlow()

    private var nextPlaybackToken = 0L

    fun playIdle() {
        state = state.copy(animationState = defaultAnimationStateFor(state.fairyTaleId))
    }

    fun playBodaet() {
        if (!state.hasKozaAnimation()) return
        state = state.copy(animationState = FairyTaleAnimationState.Bodaet(playbackToken = nextPlaybackToken()))
    }

    fun playTopTop() {
        if (!state.hasKozaAnimation()) return
        state = state.copy(animationState = FairyTaleAnimationState.TopTop(playbackToken = nextPlaybackToken()))
        _effects.tryEmit(
            FairyTalesEffect.PlaySound(
                cue = FairyTaleSoundCue(
                    id = "koza-top-top",
                    resourcePath = KOZA_TOP_TOP_SOUND_ASSET,
                    fileName = "top-top.m4a",
                ),
            ),
        )
    }

    fun onAnimationCompleted(playbackToken: Long) {
        val currentState = state.animationState
        val shouldReturnToIdle = when (currentState) {
            is FairyTaleAnimationState.Bodaet -> currentState.playbackToken == playbackToken
            is FairyTaleAnimationState.TopTop -> currentState.playbackToken == playbackToken
            else -> false
        }
        if (shouldReturnToIdle) {
            playIdle()
        }
    }

    private fun nextPlaybackToken(): Long {
        nextPlaybackToken += 1L
        return nextPlaybackToken
    }
}

private fun FairyTaleContent.toUiState(): FairyTalesUiState =
    FairyTalesUiState(
        fairyTaleId = id,
        title = title,
        description = description,
        coverColor = coverColor,
        coverRes = coverRes,
        animationAssetPath = animationAssetPath,
        animationState = defaultAnimationStateFor(id),
        storyText = storyText,
    )

private fun FairyTalesUiState.hasKozaAnimation(): Boolean =
    fairyTaleId == KOZA_FAIRY_TALE_ID

private fun defaultAnimationStateFor(fairyTaleId: String): FairyTaleAnimationState =
    if (fairyTaleId == KOZA_FAIRY_TALE_ID) {
        FairyTaleAnimationState.Idle
    } else {
        FairyTaleAnimationState.None
    }
