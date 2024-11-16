package com.lightningkite.kiteui

import java.util.Timer

actual inline fun afterTimeout(milliseconds: Long, crossinline action: () -> Unit): () -> Unit {
    // We don't accept delays on the server side.  That would be stupid.
    action()
    return {}
}