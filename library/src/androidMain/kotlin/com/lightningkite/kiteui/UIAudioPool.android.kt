package com.lightningkite.kiteui

import android.media.SoundPool
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlin.coroutines.resume

actual class UIAudioPool {

    private val soundPool = SoundPool.Builder().apply {
        setMaxStreams(4)
    }.build()

    actual suspend fun play(sound: UIAudioSegment) = suspendCoroutineCancellable { continuation ->
        val streamId = soundPool.play(sound.soundId, 1.0f, 1.0f, 0, 0, 1.0f)
        if (streamId == 0) {
            println("Error playing sound, failing silently")
            continuation.resume(Unit)
        }
        // TODO: Determine when sound has finished playing and resume
        return@suspendCoroutineCancellable {
            soundPool.stop(streamId)
        }
    }

    actual suspend fun load(source: AudioSource): UIAudioSegment {
        when (source) {
            is AudioRemote -> TODO()
            is AudioRaw -> TODO()
            is AudioLocal -> {
                return UIAudioSegment(soundPool.load(source.file.uri.path, 1))
            }
            is AudioResource -> {
                return UIAudioSegment(soundPool.load(AndroidAppContext.applicationCtx, source.resource, 1))
            }
            else -> TODO()  // Not sure why this else branch is necessary as AudioSource is a sealed class
        }
    }

    actual suspend fun unload(sound: UIAudioSegment) {
        soundPool.unload(sound.soundId)
    }
}

actual data class UIAudioSegment(val soundId: Int)