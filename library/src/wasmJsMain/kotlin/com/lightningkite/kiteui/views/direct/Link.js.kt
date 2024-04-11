package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.JsAnyNativeDelegate
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.NView2
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.calculationContext
import com.lightningkite.kiteui.views.navigator
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Navigator

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NLink(override val js: HTMLAnchorElement) : NView2<HTMLAnchorElement>() {
    var navigator: ScreenStack? = null
    var screen: Screen? = null
    var onNavigate: (suspend () -> Unit)? = null
}

@ViewDsl
actual inline fun ViewWriter.linkActual(crossinline setup: Link.() -> Unit): Unit =
    themedElementClickable("a", ::NLink) {
        this.navigator = PlatformNavigator
        js.classList.add("kiteui-stack")
        setup(Link(this))
    }

actual inline var Link.to: Screen
    get() = this.native.screen ?: Screen.Empty
    set(value) {
        this.native.screen = value
        val navigator = native.navigator!!
        navigator.routes.render(value)?.let {
            native.js.href = basePath + it.urlLikePath.render()
        }
        native.js.onclick = {
            it.preventDefault()
            if(resetsStack) {
                navigator.reset(value)
            } else {
                navigator.navigate(value)
            }
            (native.onNavigate)?.let {
                calculationContext.launchManualCancel(it)
            }
        }
    }
actual inline var Link.navigator: ScreenStack
    get() = native.navigator ?: PlatformNavigator
    set(value) {
        native.navigator = value
    }
actual inline var Link.newTab: Boolean
    get() = native.js.target == "_blank"
    set(value) {
        native.js.target = if (value) "_blank" else "_self"
    }
actual var Link.resetsStack: Boolean
    get() = native.js.getAttribute("data-resetsStack")?.toBoolean() ?: false
    set(value) {
        native.js.setAttribute("data-resetsStack", value.toString())
    }

actual fun Link.onNavigate(action: suspend () -> Unit): Unit {
    native.onNavigate = action
}