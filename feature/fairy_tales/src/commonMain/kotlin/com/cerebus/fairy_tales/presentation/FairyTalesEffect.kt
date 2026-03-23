package com.cerebus.fairy_tales.presentation

data class FairyTaleSoundCue(
    val id: String,
    val resourcePath: String,
    val fileName: String,
)

sealed interface FairyTalesEffect {
    data class PlaySound(
        val cue: FairyTaleSoundCue,
    ) : FairyTalesEffect
}
