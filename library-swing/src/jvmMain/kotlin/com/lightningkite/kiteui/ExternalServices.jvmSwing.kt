package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import java.awt.Desktop
import java.net.URI

actual fun RContext.openLink(url: String, newTab: Boolean) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}
