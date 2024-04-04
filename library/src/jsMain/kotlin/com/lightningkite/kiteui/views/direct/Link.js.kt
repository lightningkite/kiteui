package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.KiteUiNavigator
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.calculationContext
import com.lightningkite.kiteui.views.navigator
import org.w3c.dom.HTMLAnchorElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLink = HTMLAnchorElement

@ViewDsl
actual inline fun ViewWriter.linkActual(crossinline setup: Link.() -> Unit): Unit = themedElementClickable<NLink>("a") {
    this.asDynamic().__ROCK__navigator = PlatformNavigator
    classList.add("kiteui-stack")
    setup(Link(this))
}

actual inline var Link.to: KiteUiScreen
    get() = this.native.asDynamic().__ROCK__screen as KiteUiScreen
    set(value) {
        this.native.asDynamic().__ROCK__screen = value
        val navigator = (this.native.asDynamic().__ROCK__navigator as KiteUiNavigator)
        navigator.routes.render(value)?.let {
            native.href = PlatformNavigator.basePath + it.urlLikePath.render()
        }
        native.onclick = {
            it.preventDefault()
            navigator.navigate(value)
            (native.asDynamic().__ROCK__onNavigate as? suspend ()->Unit)?.let {
                calculationContext.launchManualCancel(it)
            }
        }
    }
actual inline var Link.navigator: KiteUiNavigator
    get() = native.asDynamic().__ROCK__navigator as KiteUiNavigator
    set(value) {
        native.asDynamic().__ROCK__navigator = value
    }
actual inline var Link.newTab: Boolean
    get() = native.target == "_blank"
    set(value) {
        native.target = if (value) "_blank" else "_self"
    }
actual fun Link.onNavigate(action: suspend () -> Unit): Unit {
    native.asDynamic().__ROCK__onNavigate = action
}