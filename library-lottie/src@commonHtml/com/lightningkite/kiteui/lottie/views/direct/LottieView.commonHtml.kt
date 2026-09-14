package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*
import kotlin.time.Duration

actual class LottieView actual constructor(
    context: ElementContext,
    actual val source: LottieSource,
    actual val description: String,
) : NativeElement(context) {

    internal val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    internal val _duration = RawReactive<Duration?>(ReactiveState(null))
    actual val duration: Reactive<Duration?> = _duration

    internal val _playing = Signal(false)
    actual val playing: MutableReactive<Boolean> = nativePlaying

    internal val _progress = Signal(0f)
    actual val progress: MutableReactive<Float> = nativeProgress

    internal var _loop = true
    actual var loop: Boolean
        get() = _loop
        set(value) {
            _loop = value
            nativeSetLoop(value)
        }

    internal var _speed = 1f
    actual var speed: Float
        get() = _speed
        set(value) {
            _speed = value
            nativeSetSpeed(value)
        }

    internal var _autoPlay = true
    actual var autoPlay: Boolean
        get() = _autoPlay
        set(value) { _autoPlay = value }

    internal val _completedPlay = mutableListOf<() -> Unit>()
    actual val completedPlay: Listenable = object : Listenable {
        override fun addListener(listener: () -> Unit): () -> Unit {
            _completedPlay.add(listener)
            return { _completedPlay.remove(listener) }
        }
    }

    init {
        native.tag = "div"
        native.classes.add("lottie-container")
        native.classes.add("viewDraws")
        native.setAttribute("role", "img")
        native.setAttribute("aria-label", description)

        // Store source info as data attributes for JS to pick up
        when (val value = source) {
            is LottieRemote -> {
                native.setAttribute("data-lottie-url", value.url)
            }
            is LottieRaw -> {
                native.setAttribute("data-lottie-json", value.json)
            }
        }

        // Initialize lottie on element attach
        nativeInit()
    }

    actual fun play() = nativePlay()
    actual fun pause() = nativePause()
    actual fun stop() = nativeStop()
    actual fun seekToFrame(frame: Int) = nativeSeekToFrame(frame)
    actual fun seekToProgress(progress: Float) = nativeSeekToProgress(progress)
}

// Platform-specific implementations
internal expect val LottieView.nativePlaying: MutableReactive<Boolean>
internal expect val LottieView.nativeProgress: MutableReactive<Float>
internal expect fun LottieView.nativeInit()
internal expect fun LottieView.nativeSetLoop(loop: Boolean)
internal expect fun LottieView.nativeSetSpeed(speed: Float)
internal expect fun LottieView.nativePlay()
internal expect fun LottieView.nativePause()
internal expect fun LottieView.nativeStop()
internal expect fun LottieView.nativeSeekToFrame(frame: Int)
internal expect fun LottieView.nativeSeekToProgress(progress: Float)
