@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.*
import kotlin.time.Duration.Companion.milliseconds

// External declarations for lottie-web
@JsModule("lottie-web/build/player/lottie_light.js")
@JsNonModule
external object lottie {
    fun loadAnimation(params: dynamic): dynamic
}

// Alternative: load from global if bundled via CDN
private val lottieLib: dynamic
    get() = js("(typeof lottie !== 'undefined') ? lottie : null") ?: lottie

private var LottieView.animationInstance: dynamic
    get() = native.element?.asDynamic()?._lottieAnimation
    set(value) {
        native.onElement { it.asDynamic()._lottieAnimation = value }
    }

internal actual val LottieView.nativePlaying: MutableReactive<Boolean>
    get() = object : MutableReactive<Boolean> {
        override val state: ReactiveState<Boolean>
            get() = ReactiveState(animationInstance?.isPaused != true)

        override suspend fun set(value: Boolean) {
            if (value) {
                animationInstance?.play()
            } else {
                animationInstance?.pause()
            }
            _playing.value = value
        }

        override fun addListener(listener: () -> Unit): () -> Unit {
            return AppState.animationFrame.addListener(listener)
        }
    }

internal actual val LottieView.nativeProgress: MutableReactive<Float>
    get() = object : MutableReactive<Float> {
        override val state: ReactiveState<Float>
            get() {
                val anim = animationInstance
                return if (anim != null && (anim.totalFrames as? Double ?: 0.0) > 0) {
                    ReactiveState(((anim.currentFrame as Double) / (anim.totalFrames as Double)).toFloat())
                } else {
                    ReactiveState(0f)
                }
            }

        override suspend fun set(value: Float) {
            animationInstance?.let { anim ->
                val frame = (value * (anim.totalFrames as Double)).toInt()
                anim.goToAndStop(frame, true)
            }
            _progress.value = value
        }

        override fun addListener(listener: () -> Unit): () -> Unit {
            return AppState.animationFrame.addListener(listener)
        }
    }

internal actual fun LottieView.nativeInit() {
    native.onElement { element ->
        try {
            val lib = lottieLib
            if (lib == null) {
                _state.state = ReactiveState.exception(Exception("lottie-web library not loaded"))
                return@onElement
            }

            val params: dynamic = js("{}")
            params.container = element
            params.renderer = "svg"
            params.loop = _loop
            params.autoplay = _autoPlay

            when (val src = source) {
                is LottieRemote -> {
                    params.path = src.url
                }
                is LottieRaw -> {
                    params.animationData = JSON.parse(src.json)
                }
            }

            val anim = lib.loadAnimation(params)
            animationInstance = anim

            // Set up event listeners
            anim.addEventListener("DOMLoaded") {
                val totalFrames = anim.totalFrames as? Double ?: 0.0
                val frameRate = anim.frameRate as? Double ?: 30.0
                if (totalFrames > 0 && frameRate > 0) {
                    _duration.state = ReactiveState(((totalFrames / frameRate) * 1000).toLong().milliseconds)
                }
                _state.state = ReactiveState(Unit)
            }

            anim.addEventListener("complete") {
                if (!_loop) {
                    _completedPlay.forEach { it() }
                }
            }

            anim.addEventListener("loopComplete") {
                // Loop completed, animation continues
            }

            anim.addEventListener("data_failed") {
                _state.state = ReactiveState.exception(Exception("Failed to load Lottie animation"))
            }

            // Set initial speed
            anim.setSpeed(_speed)

        } catch (e: Exception) {
            _state.state = ReactiveState.exception(e)
        }
    }
}

internal actual fun LottieView.nativeSetLoop(loop: Boolean) {
    animationInstance?.loop = loop
}

internal actual fun LottieView.nativeSetSpeed(speed: Float) {
    animationInstance?.setSpeed(speed)
}

internal actual fun LottieView.nativePlay() {
    animationInstance?.play()
    _playing.value = true
}

internal actual fun LottieView.nativePause() {
    animationInstance?.pause()
    _playing.value = false
}

internal actual fun LottieView.nativeStop() {
    animationInstance?.stop()
    _playing.value = false
    _progress.value = 0f
}

internal actual fun LottieView.nativeSeekToFrame(frame: Int) {
    animationInstance?.goToAndStop(frame, true)
}

internal actual fun LottieView.nativeSeekToProgress(progress: Float) {
    animationInstance?.let { anim ->
        val frame = (progress * (anim.totalFrames as Double)).toInt()
        anim.goToAndStop(frame, true)
    }
}
