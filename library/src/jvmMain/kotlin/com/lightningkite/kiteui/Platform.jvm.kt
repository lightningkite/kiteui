package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

@InternalKiteUi
public actual val Platform.Companion.current: Platform
    get() = Platform.Desktop
@InternalKiteUi
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = false
@InternalKiteUi
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = false
@InternalKiteUi
public actual val Platform.Companion.userAgent: String
    get() = "JVM ${Runtime.version()} ${System.getProperty("os.name") ?: "Unknown"}"

@InternalKiteUi
public actual fun setStatusBarColor(color: Color) {
}