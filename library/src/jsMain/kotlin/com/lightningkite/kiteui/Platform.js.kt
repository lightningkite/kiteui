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

// by Claude - development when running on localhost
actual val Platform.Companion.isDevelopment: Boolean
    get() = window.location.hostname.let { it == "localhost" || it == "127.0.0.1" || it == "0.0.0.0" }

actual fun setStatusBarColor(color: Color) {
}