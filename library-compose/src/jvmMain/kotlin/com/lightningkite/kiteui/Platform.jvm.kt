package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

private val osName = System.getProperty("os.name") ?: "Unknown"

public actual val Platform.Companion.current: Platform get() = Platform.Desktop
public actual val Platform.Companion.probablyAppleUser: Boolean get() = osName.startsWith("Mac")
public actual val Platform.Companion.usesTouchscreen: Boolean get() = false
public actual val Platform.Companion.userAgent: String get() = "KiteUI Compose Desktop; JVM ${Runtime.version()}; $osName"
public actual val Platform.Companion.isDevelopment: Boolean
    get() = System.getenv("KITEUI_DEVELOPMENT")?.toBoolean() ?: false

// Desktop windows have no status bar.
public actual fun setStatusBarColor(color: Color) {}
