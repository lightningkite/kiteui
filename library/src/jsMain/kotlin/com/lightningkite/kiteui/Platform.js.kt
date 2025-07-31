package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import kotlinx.browser.window

public actual val Platform.Companion.current: Platform
    get() = Platform.Web
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = window.navigator.platform.let {
        it.contains("Mac")
                || it.contains("iPhone")
                || it.contains("iPod")
                || it.contains("iPad")
    }
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = window.matchMedia("(pointer: coarse)").matches
public actual val Platform.Companion.userAgent: String
    get() = "Browser ${window.navigator.userAgent}"

public actual fun setStatusBarColor(color: Color) {
}