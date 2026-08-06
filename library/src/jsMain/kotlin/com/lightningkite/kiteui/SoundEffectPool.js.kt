package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

public actual class SoundEffectPool actual constructor(concurrency: Int) {

    // Web doesn't need the provided limit from [concurrency], so we ignore it.

    private val context = AudioContext()

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        // An AudioBufferSourceNode can only be played once so we must create a new instance every time we want to play
        // a sound
        // Browsers create an AudioContext already suspended and leave it that way until the page
        // has user activation. A suspended context's clock does not advance, so the sound is not
        // merely silent - it never reaches its end either, and `onended` never fires, which is why
        // the returned handle used to report isPlaying forever. Chrome resumes on a user gesture by
        // itself; Firefox and Safari do not, so it has to be asked for explicitly.
        //
        // Deliberately not awaited: without user activation the promise can stay pending
        // indefinitely, and awaiting it would hang play() rather than just delaying audio. The
        // source is scheduled either way and plays once the context does resume.
        if (context.state != "running") {
            context.resume().catch {
                // The browser refusing until the user interacts is normal, not a fault to report.
                if (it.message?.contains("NotAllowedError") != true) {
                    Exception("Failed to resume the audio context", it).report()
                }
            }
        }

        val bufferSource = context.createBufferSource()
        bufferSource.buffer = preloadInternal(sound)
        // An AudioBufferSourceNode has no volume control of its own, so route it through a gain node to get one.
        val gain = context.createGain()
        bufferSource.connect(gain)
        gain.connect(context.destination)
        bufferSource.start()

        // The Web Audio API offers no "is this source still running" query, so track it ourselves.
        var playing = true
        bufferSource.onended = { playing = false }

        return object : PlayingSoundEffect {
            override var volume: Float
                get() = gain.gain.value
                set(value) {
                    gain.gain.value = value
                }

            /** A source node cannot be restarted once stopped, so setting this to `true` is ignored. */
            override var isPlaying: Boolean
                get() = playing
                set(value) {
                    if (!value) stop()
                }

            override fun stop() {
                playing = false
                bufferSource.stop()
            }
        }
    }

    private val loadedMap = HashMap<AudioSource, Deferred<AudioBuffer>>()
    private suspend fun preloadInternal(sound: AudioSource): AudioBuffer {
        return loadedMap.getOrPut(sound) {
            AppScope.async {
                when (sound) {
                    is AudioRemote -> {
                        val response = window.fetch(sound.url).await()
                        val arrayBuffer = response.arrayBuffer().await()
                        context.decodeAudioData(arrayBuffer).await()
                    }

                    is AudioRaw -> {
                        val blobData = sound.data.asDynamic().arrayBuffer() as Promise<ArrayBuffer>
                        context.decodeAudioData(blobData.await()).await()
                    }

                    is AudioLocal -> {
                        val fileData = sound.file.asDynamic().arrayBuffer() as Promise<ArrayBuffer>
                        context.decodeAudioData(fileData.await()).await()
                    }

                    is AudioResource -> {
                        val response = window.fetch(basePath + sound.relativeUrl).await()
                        val arrayBuffer = response.arrayBuffer().await()
                        context.decodeAudioData(arrayBuffer).await()
                    }
                }
            }
        }.await()
    }

    public actual suspend fun preload(sound: AudioSource) {
        preloadInternal(sound)
    }

    public actual fun unload(sound: AudioSource) {
        // Not necessary for JS implementation; UIAudioPool holds no references to UIAudioSegment so they are unloaded
        // when garbage collected
    }
}

public external class AudioContext() {
    public fun createChannelMerger(numberOfInputs: Int): ChannelMergerNode
    public fun createBufferSource(): AudioBufferSourceNode
    public fun createGain(): GainNode
    public fun decodeAudioData(arrayBuffer: ArrayBuffer): Promise<AudioBuffer>
    public val destination: AudioDestinationNode
    /** "suspended", "running" or "closed". A context is created suspended. */
    public val state: String
    public fun resume(): Promise<Unit>
}

public open external class AudioNode {
    public fun connect(node: AudioNode)
    public fun connect(node: AudioNode, outputIndex: Int, inputIndex: Int)
}

public external class ChannelMergerNode : AudioNode

public external class GainNode : AudioNode {
    public val gain: AudioParam
}

public external class AudioParam {
    public var value: Float
}

public external class AudioBufferSourceNode : AudioNode {
    public var buffer: AudioBuffer
    public fun start()
    public fun stop()
    public var onended: (() -> Unit)?
}

public external class AudioBuffer {
    public val duration: Double
}

public external class AudioDestinationNode : AudioNode

public actual suspend fun AudioSource.load(): PlayableAudio {
    return suspendCancellableCoroutine { cont ->
        val native = document.createElement("audio") as HTMLAudioElement
        native.hidden = true
        native.preload = "auto"
        val obj = object : PlayableAudio {
            override var volume: Float
                get() = native.volume.toFloat()
                set(value) {
                    native.volume = value.toDouble()
                }
            override var loop: Boolean
                get() = native.loop
                set(value) { native.loop = value }
            override var isPlaying: Boolean
                get() = !native.paused
                set(value) {
                    if (value) native.play().catch {
                        if(it.message?.contains("AbortError") == true) return@catch
                        if(it.message?.contains("NotAllowedError") == true) return@catch
                        Exception("Failed to play ${this}", it).report()
                    } else native.pause()
                }

            override fun onComplete(action: () -> Unit) {
                native.onended = { action() }
            }

            override fun stop() {
                native.pause()
                native.currentTime = 0.0
            }

            override val currentTime: MutableReactive<Duration> = object : BaseListenable(), MutableReactive<Duration> {
                override suspend fun set(value: Duration) {
                    native.currentTime = value.toDouble(DurationUnit.SECONDS)
                }

                override val state get() = ReactiveState(native.currentTime.seconds)

                var remover: (() -> Unit)? = null

                override fun activate() {
                    remover = AppState.animationFrame.addListener {
                        if (!native.paused) invokeAllListeners()
                    }
                }

                override fun deactivate() {
                    remover?.invoke()
                    remover = null
                }
            }

        }
        var done = false
        native.onloadeddata = label@{
            if(done) return@label Unit
            cont.resume(obj)
            done = true
            Unit
        }
        when (val value = this) {
            is AudioRemote -> native.src = value.url
            is AudioRaw -> native.src = URL.createObjectURL(Blob(arrayOf(value.data)))
            is AudioResource -> native.src = basePath + value.relativeUrl
            is AudioLocal -> native.src = URL.createObjectURL(value.file)
        }
        native.load()
        cont.invokeOnCancellation {
            native.src = ""
        }
    }
}