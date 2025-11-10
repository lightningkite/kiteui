package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.views.toUiColor
import platform.UIKit.UIDevice

actual val Platform.Companion.probablyAppleUser: Boolean
    get() = true
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = true
actual val Platform.Companion.userAgent: String
    get() = "iOS ${UIDevice.currentDevice.model} ${UIDevice.currentDevice.systemVersion}"
actual val Platform.Companion.isSafari: Boolean
    get() = false
actual val Platform.Companion.isIOSSafari: Boolean
    get() = false

actual fun setStatusBarColor(color: Color) {
}

actual val Platform.Companion.current: Platform
    get() = Platform.iOS