package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.reactive.core.*

/**
 * JVM SSR stub implementation for LottieView.
 * On server-side, we just render a placeholder div with data attributes.
 * The actual animation will be hydrated on the client side by JavaScript.
 */

internal actual val LottieView.nativePlaying: MutableReactive<Boolean>
    get() = _playing

internal actual val LottieView.nativeProgress: MutableReactive<Float>
    get() = _progress

internal actual fun LottieView.nativeInit() {
    // For SSR, just mark as ready - the actual loading happens client-side
    _state.state = ReactiveState(Unit)
}

internal actual fun LottieView.nativeSetLoop(loop: Boolean) {
    // No-op for SSR
    native.setAttribute("data-lottie-loop", loop.toString())
}

internal actual fun LottieView.nativeSetSpeed(speed: Float) {
    // No-op for SSR
    native.setAttribute("data-lottie-speed", speed.toString())
}

internal actual fun LottieView.nativePlay() {
    // No-op for SSR
}

internal actual fun LottieView.nativePause() {
    // No-op for SSR
}

internal actual fun LottieView.nativeStop() {
    // No-op for SSR
}

internal actual fun LottieView.nativeSeekToFrame(frame: Int) {
    // No-op for SSR
}

internal actual fun LottieView.nativeSeekToProgress(progress: Float) {
    // No-op for SSR
}

internal actual fun LottieView.nativeReloadWithJson(json: String) {
    // No-op for SSR
}
