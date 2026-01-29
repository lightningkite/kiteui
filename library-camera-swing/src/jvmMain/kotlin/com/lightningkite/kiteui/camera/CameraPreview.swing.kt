// by Claude
package com.lightningkite.kiteui.camera

import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*
import java.awt.*
import javax.swing.*
import javax.swing.SwingUtilities

/**
 * SSR stub for CameraPreview.
 * Camera functionality is not available during server-side rendering.
 * This renders as an empty placeholder div.
 */
actual class CameraPreview actual constructor(context: RContext) : RView(context) {
    private val panel = JPanel(BorderLayout())
    override val native: Component = panel

    actual val hasPermissions: MutableReactive<Boolean> = Signal(false)

    actual fun onBarcode(formats: Set<BarcodeFormat>, action: (List<BarcodeResult>) -> Unit) {
        // No-op for SSR
    }

    actual suspend fun capture(): ImageLocal? = null
}
