package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics

actual object AppState {
    internal val _animationFrame = BasicListenable()
    actual val animationFrame: Listenable
        get() = _animationFrame
    internal val _windowInfo = Property(WindowStatistics(Dimension(1920f), Dimension(1080f), 1f))
    actual val windowInfo: ImmediateReadable<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Property(true)
    actual val inForeground: ImmediateReadable<Boolean>
        get() = _inForeground
    internal val _softInputOpen = Property(false)
    actual val softInputOpen: ImmediateReadable<Boolean>
        get() = _softInputOpen

}
