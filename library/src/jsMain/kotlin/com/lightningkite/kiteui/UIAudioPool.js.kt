package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
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

    actual suspend fun play(sound: UIAudioSegment) = suspendCoroutineCancellable { continuation ->
        // An AudioBufferSourceNode can only be played once so we must create a new instance every time we want to play
        // a sound
        val bufferSource = context.createBufferSource()
        bufferSource.buffer = sound
        bufferSource.connect(merger, 0, mergerInputIndexCursor)
        mergerInputIndexCursor = (mergerInputIndexCursor + 1) % numberOfStreams
        bufferSource.start()

        // TODO: Determine how long the file plays for instead of returning immediately
        continuation.resume(Unit)

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
            is AudioRaw -> TODO()
            is AudioLocal -> TODO()
            is AudioResource -> TODO()
            else -> TODO()  // Not sure why this else branch is necessary as AudioSource is a sealed class
        }
    }

    actual suspend fun unload(sound: UIAudioSegment) {
    }
}

actual typealias UIAudioSegment = AudioBuffer

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

external class AudioBuffer
external class AudioDestinationNode : AudioNode