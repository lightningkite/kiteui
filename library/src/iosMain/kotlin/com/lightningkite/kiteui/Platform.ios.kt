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

actual fun setStatusBarColor(color: Color) {
}