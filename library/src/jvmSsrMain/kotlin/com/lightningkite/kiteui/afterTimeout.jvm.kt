package com.lightningkite.kiteui

public actual inline fun afterTimeout(milliseconds: Long, crossinline action: () -> Unit): () -> Unit {
    // In SSR, we skip delayed actions entirely.
    // Executing them immediately would cause infinite recursion in code like recheckLeakAfterDelay
    // that uses afterTimeout to schedule repeated callbacks.
    return {}
}