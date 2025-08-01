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

public actual class SoundEffectPool public actual constructor(concurrency: Int) {

    private val numberOfStreams = concurrency
    private var mergerInputIndexCursor = 0

    private val context = AudioContext()
    private val merger = context.createChannelMerger(numberOfStreams).apply {
        connect(context.destination)
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        // An AudioBufferSourceNode can only be played once so we must create a new instance every time we want to play
        // a sound
        val bufferSource = context.createBufferSource()
        bufferSource.buffer = preloadInternal(sound)
        bufferSource.connect(merger, 0, mergerInputIndexCursor)
        mergerInputIndexCursor = (mergerInputIndexCursor + 1) % numberOfStreams
        bufferSource.start()

        return object : PlayingSoundEffect {
            override var volume: Float
                get() = TODO()
                set(value) {}
            override var isPlaying: Boolean
                get() = TODO()
                set(value) {}

            override fun stop() {
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

                    else -> TODO()  // Not sure why this else branch is necessary as AudioSource is a sealed class
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

@InternalKiteUi
public external class AudioContext() {
    public fun createChannelMerger(numberOfInputs: Int): ChannelMergerNode
    public fun createBufferSource(): AudioBufferSourceNode
    public fun decodeAudioData(arrayBuffer: ArrayBuffer): Promise<AudioBuffer>
    public val destination: AudioDestinationNode
}

@InternalKiteUi
public open external class AudioNode {
    public fun connect(node: AudioNode)
    public fun connect(node: AudioNode, outputIndex: Int, inputIndex: Int)
}

@InternalKiteUi
public external class ChannelMergerNode : AudioNode

@InternalKiteUi
public external class AudioBufferSourceNode : AudioNode {
    public var buffer: AudioBuffer
    public fun start()
    public fun stop()
}

@InternalKiteUi
public external class AudioBuffer {
    public val duration: Double
}

@InternalKiteUi
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
            else -> {}
        }
        native.load()
        cont.invokeOnCancellation {
            native.src = ""
        }
    }
}