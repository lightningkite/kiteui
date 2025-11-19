package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.lensing.lensListenable
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.events.Event
import kotlin.js.Promise

/**
 * AudioManager provides centralized audio management for iOS Safari to work around
 * the platform limitation that only one audio source can play at a time.
 *
 * On iOS Safari, this routes all audio through the Web Audio API, allowing multiple
 * simultaneous audio sources (videos + background audio).
 */
object AudioManager {
    private var isResuming = false
    private val pendingReconnects = mutableSetOf<String>()

    private val context: AudioContext by lazy {
        val ctx = AudioContext()
        document.addEventListener("touchstart", { _ -> requestAudioUse() })
        document.addEventListener("click", { _ -> requestAudioUse() })
        document.addEventListener("mousedown", { _ -> requestAudioUse() })
        document.addEventListener("keydown", { _ -> requestAudioUse() })
        // Listen for play events in capture phase to catch video play events
        val playOptions = js("({capture: true})")
        document.addEventListener("play", { _ -> requestAudioUse() }, playOptions)

        ctx
    }
    public fun requestAudioUse() {
        if (context.state == "suspended") {
            if (!isResuming) {
                isResuming = true
                console.log("AudioManager: User gesture detected, resuming AudioContext")
                context.resume().then(
                    onFulfilled = {
                        console.log("AudioManager: AudioContext resumed successfully, state: ${context.state}")
                        isResuming = false
                        audioEnablementChange.invokeAll()
                    },
                    onRejected = { error ->
                        console.error("AudioManager: Failed to resume AudioContext", error)
                        isResuming = false
                    }
                )
            } else {
                console.log("AudioManager: Already attempting to resume, skipping")
            }
        }
    }

    private val videoSources = mutableMapOf<String, MediaElementAudioSourceNode>()
    private val gainNodes = mutableMapOf<String, GainNode>()

    /**
     * Force using AudioContext-based audio routing.
     * True to force, false to use only if on iOS Safari..
     */
    var forceUseAudioContext: Boolean? = null
    /**
     * Whether to use AudioContext-based audio routing.
     * True on iOS Safari, false otherwise.
     */
    val shouldUseAudioContext: Boolean get() = forceUseAudioContext ?: Platform.isIOSSafari

    /**
     * Connects a video element's audio to the Web Audio API.
     * The video element itself will be muted, and audio will be routed through
     * a GainNode for volume control.
     *
     * @param videoId Unique identifier for this video element
     * @param element The HTMLVideoElement to connect
     * @return GainNode for controlling volume, or null if not using AudioContext
     */
    fun connectVideoElement(
        videoId: String,
        element: HTMLMediaElement
    ): GainNode? {
        if (!shouldUseAudioContext) return null

            // Check if source already exists for this video ID
            if (videoSources.containsKey(videoId)) {
                return gainNodes[videoId]
            }


            // Resume AudioContext if suspended (required by browser autoplay policies)
            // Note: This may fail if there's no user gesture yet, but the global event listeners will retry
            if (context.state == "suspended" && !isResuming) {
                // Don't set isResuming here - this attempt will likely fail without user gesture
                // and we don't want to block future attempts from event handlers
                try {
                    context.resume().then(
                        onFulfilled = {
                        },
                        onRejected = { error ->
                        }
                    )
                } catch (e: Throwable) {
                    console.log("AudioManager: Resume threw exception in connectVideoElement")
                }
            }

            // Create audio source from video element
            // IMPORTANT: This can only be called once per media element
            val source = context.createMediaElementSource(element)
            videoSources[videoId] = source

            // Create gain node for volume control
            val gain = context.createGain()
            gain.gain.value = 1.0 // Ensure full volume
            gainNodes[videoId] = gain

            // Connect: video -> gain -> destination (speakers)
            source.connect(gain)
            gain.connect(context.destination)

            return gain
    }

    /**
     * Disconnects and cleans up a video element's audio routing.
     *
     * @param videoId Unique identifier for the video element
     */
    fun disconnectVideoElement(videoId: String) {
        gainNodes.remove(videoId)?.disconnect()
        videoSources.remove(videoId)
    }

    /**
     * Gets the shared AudioContext instance.
     * This is the same context used for all audio routing.
     */
    fun getContext(): AudioContext = context

    /**
     * Attempts to resume the AudioContext if it's suspended.
     * This should be called from user gesture handlers (click, play, etc.)
     */
    fun tryResumeContext() {
        if (context.state == "suspended" && !isResuming) {
            isResuming = true
            context.resume().then(
                onFulfilled = {
                    isResuming = false
                },
                onRejected = { error ->
                    isResuming = false
                }
            )
        }
    }

    init {
        window.asDynamic().setMasterVolume = { volume: Double ->
            gainNodes.forEach { (_, gain) ->
                gain.gain.value = volume
            }
        }
    }
}

// Web Audio API external declarations

/**
 * The AudioContext interface represents an audio-processing graph built from
 * audio modules linked together.
 */
external class AudioContext {
    /**
     * Current state of the AudioContext: "suspended", "running", or "closed"
     */
    val state: String

    /**
     * The final destination of all audio in the context (usually the speakers)
     */
    val destination: AudioDestinationNode

    /**
     * Resume audio processing if it was suspended
     */
    fun resume(): Promise<Unit>

    /**
     * Create an audio source from an HTML media element (video or audio tag)
     */
    fun createMediaElementSource(element: HTMLMediaElement): MediaElementAudioSourceNode

    /**
     * Create a gain (volume) node
     */
    fun createGain(): GainNode

    /**
     * Create a buffer source node for playing audio buffers
     */
    fun createBufferSource(): AudioBufferSourceNode

    /**
     * Decode audio data from an ArrayBuffer into an AudioBuffer
     */
    fun decodeAudioData(arrayBuffer: org.khronos.webgl.ArrayBuffer): Promise<AudioBuffer>

    fun addEventListener(event: String, listener: (Event) -> Unit)
}

/**
 * Base class for all audio nodes in the Web Audio API graph
 */
open external class AudioNode {
    /**
     * Connect this node to another node
     */
    fun connect(node: AudioNode)

    /**
     * Connect with specific output and input indices
     */
    fun connect(node: AudioNode, outputIndex: Int, inputIndex: Int)

    /**
     * Disconnect from all connected nodes
     */
    fun disconnect()
}

/**
 * An audio source node created from an HTML media element
 */
external class MediaElementAudioSourceNode : AudioNode {
    /**
     * The HTML media element that is the source of audio
     */
    val mediaElement: HTMLMediaElement
}

/**
 * A node that controls volume (gain) of audio passing through it
 */
external class GainNode : AudioNode {
    /**
     * The gain (volume) parameter, where 1.0 is normal volume
     */
    val gain: AudioParam
}

/**
 * A node that plays audio from an AudioBuffer
 */
external class AudioBufferSourceNode : AudioNode {
    /**
     * The audio buffer to play
     */
    var buffer: AudioBuffer?

    /**
     * Whether to loop the audio
     */
    var loop: Boolean

    /**
     * Event handler called when playback ends
     */
    var onended: (() -> Unit)?

    /**
     * Start playing the audio
     */
    fun start(delay: Double = definedExternally, offset: Double = definedExternally, duration: Double = definedExternally)

    /**
     * Stop playing the audio
     */
    fun stop()
}

/**
 * Represents decoded audio data in memory
 */
external class AudioBuffer {
    /**
     * Duration of the audio in seconds
     */
    val duration: Double
}

/**
 * The final destination node (speakers/headphones)
 */
external class AudioDestinationNode : AudioNode

/**
 * Represents an audio parameter that can be controlled (like volume, frequency, etc.)
 */
external class AudioParam {
    /**
     * The current value of the parameter
     */
    var value: Double
}

/**
 * Helper to create JavaScript objects with properties
 */
private fun jsObject(init: dynamic.() -> Unit): dynamic {
    val obj = js("{}")
    obj.init()
    return obj
}

actual suspend fun RContext.enableAudio() {
    AudioManager.requestAudioUse()
}
private val audioEnablementChange by lazy {
    BasicListenable().also {
        AudioManager.getContext().addEventListener("statechange") { _ ->
            it.invokeAll()
        }
    }
}
actual val RContext.isAudioEnabled: Reactive<Boolean> get() = audioEnablementChange.lensListenable {
    AudioManager.getContext().state == "running"
}
