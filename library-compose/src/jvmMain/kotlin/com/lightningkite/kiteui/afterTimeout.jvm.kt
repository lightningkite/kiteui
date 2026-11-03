package com.lightningkite.kiteui

import javax.swing.Timer

public actual inline fun afterTimeout(milliseconds: Long, crossinline action: () -> Unit): () -> Unit {
    val timer = Timer(milliseconds.toInt()) { action() }
    timer.isRepeats = false
    timer.start()
    return { timer.stop() }
}
