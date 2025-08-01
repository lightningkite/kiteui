package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.views.toUiColor
import platform.UIKit.UIDevice

public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = true
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = true
public actual val Platform.Companion.userAgent: String
    get() = "iOS ${UIDevice.currentDevice.model} ${UIDevice.currentDevice.systemVersion}"

public actual fun setStatusBarColor(color: Color) {
}

actual val Platform.Companion.current: Platform
    get() = Platform.iOS