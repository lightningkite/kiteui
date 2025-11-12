package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.inBackground
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.ReactiveState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.*
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVFileTypeMPEGLayer3
import platform.AVFoundation.AVFileTypeWAVE
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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
        val playableAudio = this
        var onCompleteHandler: (()->Unit)? = null
        val dg = object: NSObject(), AVAudioPlayerDelegateProtocol {
            override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
                onCompleteHandler?.invoke()
//                println("keepAlive.remove($playableAudio)")
                keepAlive.remove(playableAudio)
            }

            override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
//                println("keepAlive.remove($playableAudio)")
                keepAlive.remove(playableAudio)
            }
        }
        init {
            player.delegate = dg
        }
        val native = player
        override var isPlaying: Boolean = false
            set(value) {
                field = value
                if(value) {
//                    println("keepAlive.add($playableAudio)")
                    keepAlive.add(playableAudio)
                    native.play()
                } else {
//                    println("keepAlive.remove($playableAudio)")
                    keepAlive.remove(playableAudio)
                    native.pause()
                }
            }
        override var volume: Float
            get() = native.volume
            set(value) { native.volume = value }
        override var loop: Boolean
            get() = native.numberOfLoops == -1L
            set(value) { native.numberOfLoops = if (value) -1 else 0 }

        override fun stop() {
            native.stop()
//            println("keepAlive.remove($playableAudio)")
            keepAlive.remove(playableAudio)
        }

        override fun onComplete(action: () -> Unit) {
            onCompleteHandler = action
        }

        override val currentTime: MutableReactive<Duration> = object : BaseListenable(), MutableReactive<Duration> {
            override suspend fun set(value: Duration) {
                native.playAtTime(value.toDouble(DurationUnit.SECONDS))
            }

            override val state get() = ReactiveState(native.currentTime.seconds)

            var remover: (() -> Unit)? = null

            override fun activate() {
                remover = AppState.animationFrame.addListener {
                    if (player.isPlaying()) invokeAllListeners()
                }
            }

            override fun deactivate() {
                remover?.invoke()
                remover = null
            }
        }
    }
}

private val keepAlive = HashSet<Any?>()