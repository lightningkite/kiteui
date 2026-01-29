package com.lightningkite.kiteui.models

actual class ScreenTransition private constructor() {
    actual companion object {
        actual val None: ScreenTransition = ScreenTransition()
        actual val Push: ScreenTransition = ScreenTransition()
        actual val Pop: ScreenTransition = ScreenTransition()
        actual val PullDown: ScreenTransition = ScreenTransition()
        actual val PullUp: ScreenTransition = ScreenTransition()
        actual val Fade: ScreenTransition = ScreenTransition()
        actual val GrowFade: ScreenTransition = ScreenTransition()
        actual val ShrinkFade: ScreenTransition = ScreenTransition()
    }
}
