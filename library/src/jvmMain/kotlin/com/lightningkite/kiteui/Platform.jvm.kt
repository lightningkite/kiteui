package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

actual val Platform.Companion.current: Platform
    get() = Platform.Desktop
actual val Platform.Companion.probablyAppleUser: Boolean
    get() = false
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = false
actual val Platform.Companion.userAgent: String
    get() = "JVM ${Runtime.version()} ${System.getProperty("os.name") ?: "Unknown"}"
actual val Platform.Companion.isSafari: Boolean
    get() = false
actual val Platform.Companion.isIOSSafari: Boolean
    get() = false

actual fun setStatusBarColor(color: Color) {
}