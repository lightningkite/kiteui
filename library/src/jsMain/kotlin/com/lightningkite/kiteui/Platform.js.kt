package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import kotlinx.browser.window

actual val Platform.Companion.current: Platform
    get() = Platform.Web
actual val Platform.Companion.probablyAppleUser: Boolean
    get() = window.navigator.platform.let {
        it.contains("Mac")
                || it.contains("iPhone")
                || it.contains("iPod")
                || it.contains("iPad")
    }
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = window.matchMedia("(pointer: coarse)").matches
actual val Platform.Companion.userAgent: String
    get() = "Browser ${window.navigator.userAgent}"
actual val Platform.Companion.isSafari: Boolean
    get() = window.navigator.userAgent.let {
        it.contains("Safari") && !it.contains("Chrome") && !it.contains("Chromium")
    }
actual val Platform.Companion.isIOSSafari: Boolean
    get() = probablyAppleUser && isSafari && usesTouchscreen

actual fun setStatusBarColor(color: Color) {
}