package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLAnchorElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NExternalLink = HTMLAnchorElement

@ViewDsl
actual inline fun ViewWriter.externalLinkActual(crossinline setup: ExternalLink.() -> Unit) =
    themedElementClickable<NExternalLink>("a") {
        classList.add("kiteui-stack")
        setup(ExternalLink(this))
    }

actual inline var ExternalLink.to: String
    get() = native.href
    set(value) {
        native.href = value
    }
actual inline var ExternalLink.newTab: Boolean
    get() = native.target == "_blank"
    set(value) {
        native.target = if (value) "_blank" else "_self"
    }
actual fun ExternalLink.onNavigate(action: suspend () -> Unit): Unit {
    native.onclick = {
        calculationContext.launchManualCancel(action)
    }
}