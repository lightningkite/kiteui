package com.lightningkite.kiteui

import kotlinx.browser.window

actual val Platform.Companion.current: Platform
    get() = Platform.Web
actual val Platform.Companion.probablyAppleUser: Boolean
    get() = window.navigator.userAgent.contains("Safari")
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = window.matchMedia("(pointer: coarse)").matches