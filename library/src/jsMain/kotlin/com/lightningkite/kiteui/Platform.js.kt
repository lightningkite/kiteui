package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import kotlinx.browser.window

actual val Platform.Companion.current: Platform
    get() = Platform.Web
actual val Platform.Companion.probablyAppleUser: Boolean
    get() = window.navigator.userAgent.contains("Safari")
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = window.matchMedia("(pointer: coarse)").matches

actual fun setStatusBarColor(color: Color) {
//    println("Not supported on this platform")
}