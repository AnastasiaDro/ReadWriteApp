package com.cerebus.core.sound_player

import android.media.MediaPlayer
import android.net.Uri

actual fun createSoundPlayer(): SoundPlayer = AndroidSoundPlayer()

private class AndroidSoundPlayer : SoundPlayer {
    private val playersByClipId = mutableMapOf<String, MediaPlayer>()

    override fun durationMillis(clip: SoundClip): Long? {
        return runCatching {
            val player = playersByClipId[clip.id] ?: buildPlayer(clip).also { playersByClipId[clip.id] = it }
            player.duration.toLong().takeIf { it > 0L }
        }.getOrNull()
    }

    override fun play(
        clip: SoundClip,
        restartIfPlaying: Boolean,
    ) {
        runCatching {
            val player = playersByClipId[clip.id] ?: buildPlayer(clip).also { playersByClipId[clip.id] = it }
            if (player.isPlaying) {
                if (!restartIfPlaying) return
                player.pause()
            }
            player.seekTo(0)
            player.start()
        }
    }

    override fun stop(clipId: String) {
        playersByClipId.remove(clipId)?.releaseSafely()
    }

    override fun stopAll() {
        playersByClipId.values.forEach { player ->
            player.releaseSafely()
        }
        playersByClipId.clear()
    }

    override fun release() {
        stopAll()
    }

    private fun buildPlayer(clip: SoundClip): MediaPlayer {
        val dataSource = when (val source = clip.source) {
            is SoundSource.FileUri -> normalizeFileUri(source.value)
        }
        return MediaPlayer().apply {
            setDataSource(dataSource)
            isLooping = false
            prepare()
        }
    }

    private fun normalizeFileUri(value: String): String {
        val uri = Uri.parse(value)
        return when {
            value.startsWith("file://") -> uri.path ?: value
            uri.scheme.isNullOrBlank() -> value
            else -> value
        }
    }

    private fun MediaPlayer.releaseSafely() {
        runCatching {
            if (isPlaying) {
                stop()
            }
        }
        release()
    }
}
