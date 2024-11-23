package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

actual val Platform.Companion.current: Platform
    get() = Platform.Desktop
actual val Platform.Companion.probablyAppleUser: Boolean
    get() = false
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = false

actual fun setStatusBarColor(color: Color) {
}