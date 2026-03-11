package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import java.awt.Desktop
import java.awt.Toolkit

actual val Platform.Companion.current: Platform
    get() = Platform.Desktop

actual val Platform.Companion.probablyAppleUser: Boolean
    get() = System.getProperty("os.name").contains("Mac", ignoreCase = true)

actual val Platform.Companion.usesTouchscreen: Boolean
    get() = false

actual val Platform.Companion.userAgent: String
    get() = "Java ${System.getProperty("java.version")} on ${System.getProperty("os.name")} ${System.getProperty("os.version")}"

// by Claude - desktop Swing apps are always development
actual val Platform.Companion.isDevelopment: Boolean
    get() = true

actual fun setStatusBarColor(color: Color) {
    // No-op for desktop - no status bar to set
    // Could potentially be used for window title bar theming in the future
}
