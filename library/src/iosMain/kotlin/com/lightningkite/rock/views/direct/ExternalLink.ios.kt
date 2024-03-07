package com.lightningkite.rock.views.direct

import com.lightningkite.rock.launchManualCancel
import com.lightningkite.rock.views.ViewDsl
import com.lightningkite.rock.views.ViewWriter
import com.lightningkite.rock.views.calculationContext
import platform.UIKit.UIControlEventTouchUpInside

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NExternalLink = NativeLink

@ViewDsl
actual inline fun ViewWriter.externalLinkActual(crossinline setup: ExternalLink.() -> Unit): Unit = element(NativeLink()) {
    handleThemeControl(this) {
        setup(ExternalLink(this))
    }
}

actual inline var ExternalLink.to: String
    get() = native.toUrl ?: ""
    set(value) {
        native.toUrl = value
    }
actual inline var ExternalLink.newTab: Boolean
    get() = native.newTab
    set(value) {
        native.newTab = value
    }
actual fun ExternalLink.onNavigate(action: suspend () -> Unit): Unit {
    native.onNavigate = action
}