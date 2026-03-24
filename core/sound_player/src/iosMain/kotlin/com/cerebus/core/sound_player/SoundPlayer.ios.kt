package com.cerebus.core.sound_player

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

actual fun createSoundPlayer(): SoundPlayer = IosSoundPlayer()

@OptIn(ExperimentalForeignApi::class)
private class IosSoundPlayer : SoundPlayer {
    private val playersByClipId = mutableMapOf<String, AVAudioPlayer>()

    override fun durationMillis(clip: SoundClip): Long? {
        return runCatching {
            val player = playersByClipId[clip.id] ?: buildPlayer(clip).also { newPlayer ->
                playersByClipId[clip.id] = newPlayer
            }
            (player.duration * 1000.0).toLong().takeIf { it > 0L }
        }.getOrNull()
    }

    override fun play(
        clip: SoundClip,
        restartIfPlaying: Boolean,
    ) {
        runCatching {
            val player = playersByClipId[clip.id] ?: buildPlayer(clip).also { newPlayer ->
                playersByClipId[clip.id] = newPlayer
            }
            if (player.playing && !restartIfPlaying) return
            player.stop()
            player.currentTime = 0.0
            player.prepareToPlay()
            player.play()
        }
    }

    override fun stop(clipId: String) {
        playersByClipId.remove(clipId)?.let { player ->
            player.stop()
        }
    }

    override fun stopAll() {
        playersByClipId.values.forEach { player ->
            player.stop()
        }
        playersByClipId.clear()
    }

    override fun release() {
        stopAll()
    }

    private fun buildPlayer(clip: SoundClip): AVAudioPlayer {
        val url = when (val source = clip.source) {
            is SoundSource.FileUri -> source.value.toNsUrl()
        }
        return requireNotNull(AVAudioPlayer(contentsOfURL = url, error = null))
    }

    private fun String.toNsUrl(): NSURL =
        if (startsWith("file://")) {
            requireNotNull(NSURL.URLWithString(this))
        } else {
            NSURL.fileURLWithPath(this)
        }
}
