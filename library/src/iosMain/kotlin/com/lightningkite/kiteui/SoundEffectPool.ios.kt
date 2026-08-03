package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.utils.SuspendCache
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
import platform.Foundation.NSItemProvider
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** The raw bytes of a sound effect plus the UTI hint AVAudioPlayer needs to decode it. */
private class LoadedSound(val data: NSData, val fileTypeHint: String?)

/**
 * The AVFoundation file-type UTI for a file extension, or null when it is not one we recognise.
 *
 * Null is fine: the hint only helps AVAudioPlayer skip sniffing, and it falls back to inspecting the
 * data itself. Guessing wrong would be worse than not guessing.
 */
private fun fileTypeHint(extension: String): String? = when (extension.lowercase()) {
    "mp3" -> AVFileTypeMPEGLayer3
    "m4a", "mp4", "aac" -> AVFileTypeMPEG4
    "wav", "wave" -> AVFileTypeWAVE
    else -> null
}

/** As [fileTypeHint], but from the MIME type a [Blob] carries rather than from a file extension. */
private fun mimeFileTypeHint(mimeType: String): String? = when (mimeType.substringBefore(';').trim().lowercase()) {
    "audio/mpeg", "audio/mp3" -> AVFileTypeMPEGLayer3
    "audio/mp4", "audio/m4a", "audio/aac", "audio/x-m4a" -> AVFileTypeMPEG4
    "audio/wav", "audio/wave", "audio/x-wav" -> AVFileTypeWAVE
    else -> null
}

/**
 * Reads a picked file's bytes.
 *
 * NSItemProvider hands its data over asynchronously and only for a type, so the registered type
 * identifier is used when the reference does not carry a suggested one.
 *
 * Uses `loadDataRepresentationForTypeIdentifier`, not the `ForContentType` overload that
 * [FileReference.text] uses: the latter is iOS 16+, while this module's deployment target is 14.0, so
 * on iOS 14 and 15 it is an unrecognized selector - a crash rather than a failed load.
 */
private suspend fun FileReference.data(): NSData {
    val typeIdentifier = suggestedType?.identifier
        ?: (provider.registeredTypeIdentifiers.firstOrNull() as? String)
        ?: "public.data"
    return suspendCoroutine { continuation ->
        provider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
            if (data != null) continuation.resume(data)
            else continuation.resumeWithException(
                Exception("Could not read audio from the picked file: ${error?.localizedDescription ?: "no data"}")
            )
        }
    }
}

/**
 * Reads a sound effect's bytes and works out the decode hint for them.
 *
 * Shared by [SoundEffectPool] and [AudioSource.load] so the four source kinds are handled in exactly
 * one place - they had drifted into two copies with different gaps, each with its own `TODO()`.
 *
 * Everything is decoded from bytes rather than handed to AVAudioPlayer as a URL. That is required
 * for raw and picked-file sources, and it is also the only thing that works for a remote one:
 * `AVAudioPlayer(contentsOfURL:)` is documented for file URLs and silently fails on http(s).
 */
private suspend fun loadSound(source: AudioSource): LoadedSound = when (source) {
    // dataWithContentsOfURL blocks, so these two go through inBackground.
    is AudioRemote -> inBackground {
        LoadedSound(
            data = NSData.dataWithContentsOfURL(NSURL(string = source.url))
                ?: throw Exception("Could not load audio from ${source.url}"),
            fileTypeHint = fileTypeHint(source.url.substringAfterLast('.'))
        )
    }

    is AudioResource -> inBackground {
        val url = NSBundle.mainBundle.URLForResource(source.name, source.extension)
            ?: throw Exception("Could not find the audio in the bundle ${source.name} / ${source.extension}")
        LoadedSound(
            data = NSData.dataWithContentsOfURL(url)
                ?: throw Exception("Could not load audio at $url"),
            fileTypeHint = fileTypeHint(source.extension)
        )
    }

    // NSItemProvider delivers its bytes through its own callback, so this is already off the main
    // thread - and inBackground takes a blocking lambda, which cannot host a suspend call.
    is AudioLocal -> LoadedSound(
        data = source.file.data(),
        fileTypeHint = fileTypeHint(source.file.suggestedType?.preferredFilenameExtension ?: "")
    )

    // Already in memory: a Blob holds its bytes as NSData on this platform, so there is nothing to
    // read, nothing to stage, and nothing to move off the main thread.
    is AudioRaw -> LoadedSound(
        data = source.data.data,
        fileTypeHint = mimeFileTypeHint(source.data.type)
    )
}

public actual class SoundEffectPool actual constructor(concurrency: Int) {

    // Sound effects are short and frequently overlap (e.g. rapid button taps), so each play() gets
    // its own AVAudioPlayer instance; only the decoded bytes are shared through this cache.
    private val loaded = SuspendCache<AudioSource, LoadedSound>(compute = ::loadSound)

    public actual suspend fun preload(sound: AudioSource) {
        loaded.get(sound)
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        val sound = loaded.get(sound)
        val player = AVAudioPlayer(data = sound.data, fileTypeHint = sound.fileTypeHint, null)
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
        loaded.forget(sound)
    }
}


public actual suspend fun AudioSource.load(): PlayableAudio {
    AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
    AVAudioSession.sharedInstance().setActive(true, null)
    val loaded = loadSound(this)
    val player = AVAudioPlayer(data = loaded.data, fileTypeHint = loaded.fileTypeHint, null)
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