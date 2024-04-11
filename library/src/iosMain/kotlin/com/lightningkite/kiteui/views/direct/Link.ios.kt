package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
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

actual inline var Link.to: Screen
    get() = native.toScreen ?: Screen.Empty
    set(value) {
        native.toScreen = value
    }
actual inline var Link.navigator: ScreenStack
    get() = native.onNavigator ?: PlatformNavigator
    set(value) {
        native.onNavigator = value
    }
actual inline var Link.newTab: Boolean
    get() = native.newTab
    set(value) {
        native.newTab = value
    }
actual var Link.resetsStack: Boolean
    get() = native.resetsStack
    set(value) {
        native.resetsStack = value
    }

actual fun Link.onNavigate(action: suspend () -> Unit): Unit {
    native.onNavigate = action
}