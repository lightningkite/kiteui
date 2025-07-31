package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.direct.KeyCodeWithModifiers
import com.lightningkite.signal.*
import kotlinx.coroutines.CoroutineScope

public actual object AppState {
    internal val _animationFrame = BasicListenable()
    public actual val animationFrame: Listenable
        get() = _animationFrame
    internal val _windowInfo = Property(WindowStatistics(1920.px, 1080.px, 1f))
    public actual val windowInfo: ImmediateReadable<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Property(true)
    public actual val inForeground: ImmediateReadable<Boolean>
        get() = _inForeground
    internal val _softInputOpen = Property(false)
    public actual val softInputOpen: ImmediateReadable<Boolean>
        get() = _softInputOpen

    public actual fun keepScreenOn(scope: CoroutineScope) {
        // Nothing to do; we're server-side
    }
    public actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit = {}
}
