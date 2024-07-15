package com.lightningkite.kiteui

import com.lightningkite.kiteui.dom.HTMLAudioElement
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.basePath
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.js.Promise

actual class SoundEffectPool actual constructor(concurrency: Int) {

    private val numberOfStreams = concurrency
    private var mergerInputIndexCursor = 0

    private val context = AudioContext()
    private val merger = context.createChannelMerger(numberOfStreams).apply {
        connect(context.destination)
    }

    actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
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

    private val loadedMap = HashMap<AudioSource, Async<AudioBuffer>>()
    private suspend fun preloadInternal(sound: AudioSource): AudioBuffer {
        return loadedMap.getOrPut(sound) {
            asyncGlobal {
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

    actual suspend fun preload(sound: AudioSource) {
        preloadInternal(sound)
    }

    actual fun unload(sound: AudioSource) {
        // Not necessary for JS implementation; UIAudioPool holds no references to UIAudioSegment so they are unloaded
        // when garbage collected
    }
}

external class AudioContext() {
    fun createChannelMerger(numberOfInputs: Int): ChannelMergerNode
    fun createBufferSource(): AudioBufferSourceNode
    fun decodeAudioData(arrayBuffer: ArrayBuffer): Promise<AudioBuffer>
    val destination: AudioDestinationNode
}

open external class AudioNode {
    fun connect(node: AudioNode)
    fun connect(node: AudioNode, outputIndex: Int, inputIndex: Int)
}

external class ChannelMergerNode : AudioNode

external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer
    fun start()
    fun stop()
}

external class AudioBuffer {
    val duration: Double
}

external class AudioDestinationNode : AudioNode

actual suspend fun AudioSource.load(): PlayableAudio {
    return suspendCoroutineCancellable { cont ->
        val native = document.createElement("audio") as HTMLAudioElement
        native.hidden = true
        val obj = object : PlayableAudio {
            override var volume: Float
                get() = native.volume.toFloat()
                set(value) {
                    native.volume = value.toDouble()
                }
            override var isPlaying: Boolean
                get() = !native.paused
                set(value) {
                    println("Play ${this@load} started")
                    if (value) native.play() else native.pause()
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
            null -> native.src = ""
            is AudioRemote -> native.src = value.url
            is AudioRaw -> native.src = URL.createObjectURL(Blob(arrayOf(value.data)))
            is AudioResource -> native.src = basePath + value.relativeUrl
            is AudioLocal -> native.src = URL.createObjectURL(value.file)
            else -> {}
        }
        native.load()
        return@suspendCoroutineCancellable {
            println("Cancelled loading of ${this@load}.")
            native.src = ""
        }
    }
}