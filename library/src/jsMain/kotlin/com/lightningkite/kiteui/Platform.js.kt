package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import kotlinx.browser.window

@InternalKiteUi
public actual val Platform.Companion.current: Platform
    get() = Platform.Web
@InternalKiteUi
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = window.navigator.platform.let {
        it.contains("Mac")
                || it.contains("iPhone")
                || it.contains("iPod")
                || it.contains("iPad")
    }
@InternalKiteUi
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = window.matchMedia("(pointer: coarse)").matches
@InternalKiteUi
public actual val Platform.Companion.userAgent: String
    get() = "Browser ${window.navigator.userAgent}"

@InternalKiteUi
public actual fun setStatusBarColor(color: Color) {
}