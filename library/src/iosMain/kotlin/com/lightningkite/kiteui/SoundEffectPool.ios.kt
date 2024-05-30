package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.inBackground
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.*
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVFileTypeMPEGLayer3
import platform.AVFoundation.AVFileTypeWAVE
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.darwin.NSObject

actual class SoundEffectPool actual constructor(concurrency: Int) {
    actual suspend fun preload(sound: AudioSource) {
    }

    actual suspend fun play(sound: AudioSource): PlayingSoundEffect = object : PlayingSoundEffect {
        override var isPlaying: Boolean
            get() = false
            set(value) {}
        override var volume: Float
            get() = 0f
            set(value) {}

        override fun stop() {

        }
    }

    actual fun unload(sound: AudioSource) {
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun AudioSource.load(): PlayableAudio {
    AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
    AVAudioSession.sharedInstance().setActive(true, null)
    val player = inBackground {
        when (val value = this) {
            is AudioLocal -> TODO()
            is AudioRaw -> TODO()
            is AudioRemote -> AVAudioPlayer(
                contentsOfURL = NSURL(string = value.url),
                fileTypeHint = when (value.url.substringAfterLast('.')) {
                    "mp3" -> AVFileTypeMPEGLayer3
                    "m4a" -> AVFileTypeMPEG4
                    "wav" -> AVFileTypeWAVE
                    else -> null
                },
                null
            )

            is AudioResource -> AVAudioPlayer(
                contentsOfURL = NSBundle.mainBundle.URLForResource(value.name, value.extension)
                    ?: throw Exception("Could not find the audio in the bundle ${value.name} / ${value.extension}"),
                fileTypeHint = when (value.extension) {
                    "mp3" -> AVFileTypeMPEGLayer3
                    "m4a" -> AVFileTypeMPEG4
                    "wav" -> AVFileTypeWAVE
                    else -> null
                },
                null
            )

            else -> TODO()
        }
    }
    return object : PlayableAudio {
        var onCompleteHandler: (()->Unit)? = null
        val dg = object: NSObject(), AVAudioPlayerDelegateProtocol {
            override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
                onCompleteHandler?.invoke()
            }
            init {
                player.delegate = this
            }
        }
        val native = player
        override var isPlaying: Boolean
            get() = native.rate > 0.0
            set(value) {
                if(value) native.play()
                else native.pause()
            }
        override var volume: Float
            get() = native.volume
            set(value) { native.volume = value }

        override fun stop() {
            native.stop()
        }

        override fun onComplete(action: () -> Unit) {
            onCompleteHandler = action
        }
    }
}