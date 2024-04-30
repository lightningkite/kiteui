package com.lightningkite.kiteui.views.direct

import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.calculationContext
import java.util.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NExternalLink = LinkFrameLayout

actual var ExternalLink.to: String
    get() {
        return (native.tag as? Pair<UUID, String>)?.second ?: ""
    }
    set(value) {
        native.tag = UUID.randomUUID() to value
        native.setOnClickListener { view ->
            ExternalServices.openTab(value)
        }
    }
actual var ExternalLink.newTab: Boolean
    get() {
        return native.tag as? Boolean ?: false
    }
    set(value) {
        native.tag = value
    }
actual fun ExternalLink.onNavigate(action: suspend () -> Unit): Unit {
    native.onNavigate = action
}

@ViewDsl
actual inline fun ViewWriter.externalLinkActual(crossinline setup: ExternalLink.() -> Unit) {
    viewElement(factory = ::LinkFrameLayout, wrapper = ::ExternalLink) {
        handleThemeControl(native) {
            setup(this)
        }
    }
}