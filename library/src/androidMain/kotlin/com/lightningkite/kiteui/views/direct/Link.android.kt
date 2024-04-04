package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.KiteUiNavigator
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.calculationContext
import com.lightningkite.kiteui.views.navigator
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLink = LinkFrameLayout

actual var Link.to: KiteUiScreen
    get() = TODO()
    set(value) {
        native.setOnClickListener {
            native.navigator.navigate(value)
            calculationContext.launchManualCancel { native.onNavigate() }
        }
    }
actual var Link.navigator: KiteUiNavigator
    get() = native.navigator
    set(value) {
        native.navigator = value
    }
actual var Link.newTab: Boolean
    get() {
        return false
    }
    set(value) {
        Timber.d("New Tab called with value $value")
    }
actual fun Link.onNavigate(action: suspend () -> Unit): Unit {
    native.onNavigate = action
}

@ViewDsl
actual inline fun ViewWriter.linkActual(crossinline setup: Link.() -> Unit) {
    return viewElement(factory = ::LinkFrameLayout, wrapper = ::Link) {
        native.navigator = PlatformNavigator
        // OnClickListener may not be set until after handleTheme() is called, so we must manually set isClickable for
        // the RippleDrawable to be added to the background
        native.isClickable = true
        handleThemeControl(native) {
            setup(this)
        }
    }
}
