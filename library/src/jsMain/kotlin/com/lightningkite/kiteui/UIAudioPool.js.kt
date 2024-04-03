package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PlatformNavigator
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import kotlin.coroutines.resume
import kotlin.js.Promise

actual class UIAudioPool {

    private val numberOfStreams = 4
    private var mergerInputIndexCursor = 0

    private val context = AudioContext()
    private val merger = context.createChannelMerger(numberOfStreams).apply {
        connect(context.destination)
    }

    private val gainNodes = (0..<numberOfStreams).map {
        context.createGain().apply {
            connect(merger, 0, it)
        }
    }

    actual suspend fun play(sound: UIAudioSegment, gain: Float) = suspendCoroutineCancellable { continuation ->
        // An AudioBufferSourceNode can only be played once so we must create a new instance every time we want to play
        // a sound
        val bufferSource = context.createBufferSource()
        val gainNode = gainNodes[mergerInputIndexCursor]

        bufferSource.buffer = sound
        bufferSource.connect(gainNode)
        gainNode.gain.value = gain

        mergerInputIndexCursor = (mergerInputIndexCursor + 1) % numberOfStreams
        bufferSource.start()

        afterTimeout((sound.duration * 1000).toLong()) {
            continuation.resume(Unit)
        }

        return@suspendCoroutineCancellable {
            bufferSource.stop()
        }
    }

    actual suspend fun load(source: AudioSource): UIAudioSegment {
        when (source) {
            is AudioRemote -> {
                val response = window.fetch(source.url).await()
                val arrayBuffer = response.arrayBuffer().await()
                return context.decodeAudioData(arrayBuffer).await()
            }
            is AudioRaw -> {
                val blobData = source.data.asDynamic().arrayBuffer() as Promise<ArrayBuffer>
                return context.decodeAudioData(blobData.await()).await()
            }
            is AudioLocal -> {
                val fileData = source.file.asDynamic().arrayBuffer() as Promise<ArrayBuffer>
                return context.decodeAudioData(fileData.await()).await()
            }
            is AudioResource -> {
                val response = window.fetch(PlatformNavigator.basePath + source.relativeUrl).await()
                val arrayBuffer = response.arrayBuffer().await()
                return context.decodeAudioData(arrayBuffer).await()
            }
            else -> TODO()  // Not sure why this else branch is necessary as AudioSource is a sealed class
        }
    }

    actual suspend fun unload(sound: UIAudioSegment) {
        // Not necessary for JS implementation; UIAudioPool holds no references to UIAudioSegment so they are unloaded
        // when garbage collected
    }
}

actual typealias UIAudioSegment = AudioBuffer

external class AudioContext() {
    fun createChannelMerger(numberOfInputs: Int): ChannelMergerNode
    fun createBufferSource(): AudioBufferSourceNode
    fun createGain(): GainNode
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

external class AudioDestinationNode : AudioNode

external class GainNode : AudioNode {
    val gain: AudioParam
}

external class AudioBuffer {
    val duration: Double
}

external class AudioParam {
    var value: Float
}