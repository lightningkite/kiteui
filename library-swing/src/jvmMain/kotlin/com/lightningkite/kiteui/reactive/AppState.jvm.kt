package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import javax.swing.Timer

actual object AppState {
    internal val _animationFrame = BasicListenable()
    private var animationTimer: Timer? = null

    actual val animationFrame: Listenable
        get() {
            // Start the animation timer on first access (lazy initialization)
            if (animationTimer == null) {
                animationTimer = Timer(16) { // ~60fps
                    _animationFrame.invokeAll()
                }.apply {
                    isRepeats = true
                    start()
                }
            }
            return _animationFrame
        }
    internal val _windowInfo = Signal(WindowStatistics(1920.px, 1080.px, 1f))
    actual val windowInfo: ReactiveValue<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Signal(true)
    actual val inForeground: ReactiveValue<Boolean>
        get() = _inForeground
    internal val _softInputOpen = Signal(false)
    actual val softInputOpen: ReactiveValue<Boolean>
        get() = _softInputOpen

    actual fun keepScreenOn(scope: CoroutineScope) {
        // Nothing to do; we're server-side
    }
    actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit = {}
}
