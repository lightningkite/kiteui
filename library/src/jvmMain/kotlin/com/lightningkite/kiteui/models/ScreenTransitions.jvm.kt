package com.lightningkite.kiteui.models

actual class ScreenTransition {
    actual companion object {
        actual val None: ScreenTransition
            get() = ScreenTransition()
        actual val Push: ScreenTransition
            get() = ScreenTransition()
        actual val Pop: ScreenTransition
            get() = ScreenTransition()
        actual val PullDown: ScreenTransition
            get() = ScreenTransition()
        actual val PullUp: ScreenTransition
            get() = ScreenTransition()
        actual val Fade: ScreenTransition
            get() = ScreenTransition()
        actual val GrowFade: ScreenTransition
            get() = ScreenTransition()
        actual val ShrinkFade: ScreenTransition
            get() = ScreenTransition()
    }
}