package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.inBackground
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.ReactiveState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import platform.AVFAudio.*
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVFileTypeMPEGLayer3
import platform.AVFoundation.AVFileTypeWAVE
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** The raw bytes of a sound effect plus the UTI hint AVAudioPlayer needs to decode it. */
private class LoadedSound(val data: NSData, val fileTypeHint: String?)

private fun fileTypeHint(extension: String): String? = when (extension) {
    "mp3" -> AVFileTypeMPEGLayer3
    "m4a" -> AVFileTypeMPEG4
    "wav" -> AVFileTypeWAVE
    else -> null
}

public actual class SoundEffectPool actual constructor(concurrency: Int) {

    // Sound effects are short and frequently overlap (e.g. rapid button taps), so each play()
    // gets its own AVAudioPlayer instance; only the decoded bytes are shared via this cache.
    private val loadedMap = HashMap<AudioSource, Deferred<LoadedSound>>()

    public actual suspend fun preload(sound: AudioSource) {
        preloadInternal(sound)
    }

    private suspend fun preloadInternal(source: AudioSource): LoadedSound {
        return loadedMap.getOrPut(source) {
            AppScope.async {
                inBackground {
                    when (source) {
                        is AudioRemote -> LoadedSound(
                            data = NSData.dataWithContentsOfURL(NSURL(string = source.url))
                                ?: throw Exception("Could not load audio from ${source.url}"),
                            fileTypeHint = fileTypeHint(source.url.substringAfterLast('.'))
                        )

                        is AudioResource -> {
                            val url = NSBundle.mainBundle.URLForResource(source.name, source.extension)
                                ?: throw Exception("Could not find the audio in the bundle ${source.name} / ${source.extension}")
                            LoadedSound(
                                data = NSData.dataWithContentsOfURL(url)
                                    ?: throw Exception("Could not load audio at $url"),
                                fileTypeHint = fileTypeHint(source.extension)
                            )
                        }

                        is AudioLocal -> TODO()
                        is AudioRaw -> TODO()
                    }
                }
            }
        }.await()
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        val loaded = preloadInternal(sound)
        val player = AVAudioPlayer(data = loaded.data, fileTypeHint = loaded.fileTypeHint, null)
        return object : PlayingSoundEffect {
            @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
            val dg = run {
                // Use a weak reference to avoid a retain cycle between the delegate and this effect.
                val weakSelf = kotlin.native.ref.WeakReference(this)
                object : NSObject(), AVAudioPlayerDelegateProtocol {
                    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
                        weakSelf.get()?.let { keepAlive.remove(it) }
                    }

                    override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
                        weakSelf.get()?.let { keepAlive.remove(it) }
                    }
                }
            }

            init {
                player.delegate = dg
                // Keep this effect (and transitively its delegate) alive for as long as it's playing.
                keepAlive.add(this)
                player.play()
            }

            override var isPlaying: Boolean
                get() = player.isPlaying()
                set(value) {
                    if (value) player.play() else player.pause()
                }
            override var volume: Float
                get() = player.volume
                set(value) {
                    player.volume = value
                }

            override fun stop() {
                player.stop()
                keepAlive.remove(this)
            }
        }
    }

    public actual fun unload(sound: AudioSource) {
        loadedMap.remove(sound)
    }
}


public actual suspend fun AudioSource.load(): PlayableAudio {
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
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        val dg = run {
            // Use weak reference to avoid retain cycle
            val weakSelf = kotlin.native.ref.WeakReference(this)
            object: NSObject(), AVAudioPlayerDelegateProtocol {
                override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
                    weakSelf.get()?.let { audio ->
                        audio.onCompleteHandler?.invoke()
                        audio.isPlaying = false
//                        println("keepAlive.remove($audio)")
                        keepAlive.remove(audio)
                    }
                }

                override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
//                    println("keepAlive.remove($playableAudio)")
                    weakSelf.get()?.let { keepAlive.remove(it) }
                }
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