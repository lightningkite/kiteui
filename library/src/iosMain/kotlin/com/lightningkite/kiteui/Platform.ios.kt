package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.views.toUiColor
import platform.UIKit.UIDevice

@InternalKiteUi
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = true
@InternalKiteUi
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = true
@InternalKiteUi
public actual val Platform.Companion.userAgent: String
    get() = "iOS ${UIDevice.currentDevice.model} ${UIDevice.currentDevice.systemVersion}"

@InternalKiteUi
public actual fun setStatusBarColor(color: Color) {
}

@InternalKiteUi
public actual val Platform.Companion.current: Platform
    get() = Platform.iOS