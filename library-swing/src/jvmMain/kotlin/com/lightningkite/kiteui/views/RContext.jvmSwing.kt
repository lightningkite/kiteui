package com.lightningkite.kiteui.views

import java.awt.Window
import java.lang.ref.WeakReference
import javax.swing.JFrame
import javax.swing.SwingUtilities

actual class RContext(window: Window) : RContextHelper() {
    private val windowRef = WeakReference(window)

    /**
     * Returns the window if it's still alive, or throws IllegalStateException if destroyed.
     * Use this property when you absolutely need the window and can't continue without it.
     */
    val window: Window
        get() = windowRef.get() ?: throw IllegalStateException("Window has been destroyed")

    /**
     * Returns the window if it's still alive, or null if destroyed.
     * Use this property when you want to check if the window is still available.
     */
    val windowOrNull: Window?
        get() = windowRef.get()

    actual override val darkMode: Boolean?
        get() {
            // TODO: Detect system dark mode on macOS/Windows/Linux
            // For now, return null (let KiteUI use default theme)
            return null
        }

    actual fun split(): RContext = windowOrNull?.let {
        RContext(it).also { it.addons.putAll(addons) }
    } ?: throw IllegalStateException("Cannot split RContext: Window has been destroyed")

    actual var immersiveMode: Boolean = false
        set(value) {
            field = value
            // Could potentially hide window decorations here
        }

    actual companion object {}
}

/**
 * Helper to ensure code runs on the Swing EDT (Event Dispatch Thread).
 * Similar to Android's runOnUiThread.
 */
fun runOnUiThread(action: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        action()
    } else {
        SwingUtilities.invokeLater(action)
    }
}
