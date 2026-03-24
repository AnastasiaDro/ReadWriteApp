package com.cerebus.core.sound_player

sealed class SoundSource {
    data class FileUri(
        val value: String,
    ) : SoundSource()
}

data class SoundClip(
    val id: String,
    val source: SoundSource,
)

interface SoundPlayer {
    fun durationMillis(clip: SoundClip): Long?

    fun play(
        clip: SoundClip,
        restartIfPlaying: Boolean = true,
    )

    fun stop(clipId: String)

    fun stopAll()

    fun release()
}

expect fun createSoundPlayer(): SoundPlayer
