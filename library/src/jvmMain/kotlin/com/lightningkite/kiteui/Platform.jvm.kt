package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

public actual val Platform.Companion.current: Platform
    get() = Platform.Desktop
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = false
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = false
public actual val Platform.Companion.userAgent: String
    get() = "JVM ${Runtime.version()} ${System.getProperty("os.name") ?: "Unknown"}"

public actual fun setStatusBarColor(color: Color) {
}