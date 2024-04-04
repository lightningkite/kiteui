package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.KiteUiNavigator
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.navigator

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLink = NativeLink

@ViewDsl
actual inline fun ViewWriter.linkActual(crossinline setup: Link.() -> Unit): Unit = element(NativeLink()) {
    handleThemeControl(this) {
        setup(Link(this))
        onNavigator = PlatformNavigator
    }
}

actual inline var Link.to: KiteUiScreen
    get() = native.toScreen ?: KiteUiScreen.Empty
    set(value) {
        native.toScreen = value
    }
actual inline var Link.navigator: KiteUiNavigator
    get() = native.onNavigator ?: PlatformNavigator
    set(value) {
        native.onNavigator = value
    }
actual inline var Link.newTab: Boolean
    get() = native.newTab
    set(value) {
        native.newTab = value
    }
actual fun Link.onNavigate(action: suspend () -> Unit): Unit {
    native.onNavigate = action
}