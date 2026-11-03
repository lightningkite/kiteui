package com.lightningkite.kiteui

import javax.swing.SwingUtilities

// Compose Desktop runs on the Swing event thread, and Dispatchers.Main (kotlinx-coroutines-swing) is it.

public actual inline fun onMainThread(crossinline action: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeLater { action() }
}

public actual fun assertMainThread() {
    check(SwingUtilities.isEventDispatchThread()) { "Must be called on the main (Swing event) thread, not ${Thread.currentThread().name}" }
}
