package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

actual class DismissBackground actual constructor(context: RContext) : RView(context) {
    override val native = JPanel().apply {
        // Semi-transparent black background
        background = Color(0, 0, 0, 80)
        isOpaque = true
    }

    actual fun onClick(action: suspend () -> Unit) {
        native.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                launch { action() }
            }
        })
    }
}
