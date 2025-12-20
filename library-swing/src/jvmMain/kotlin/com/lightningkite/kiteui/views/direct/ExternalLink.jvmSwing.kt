package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.JPanel

actual class ExternalLink actual constructor(context: RContext) : RView(context) {
    override val native = JPanel().apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

    actual var enabled: Boolean = true
        set(value) {
            field = value
            native.isEnabled = value
            refreshTheming()
        }

    actual var to: String? = null
        set(value) {
            field = value
            // Remove all existing mouse listeners
            native.mouseListeners.forEach { native.removeMouseListener(it) }

            // Add new click handler
            native.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (enabled && e.button == MouseEvent.BUTTON1) {
                        launch {
                            onNavigate.invoke()
                            value?.let { url ->
                                try {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().browse(URI(url))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            })
        }

    actual var newTab: Boolean = false

    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit) {
        onNavigate = action
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
