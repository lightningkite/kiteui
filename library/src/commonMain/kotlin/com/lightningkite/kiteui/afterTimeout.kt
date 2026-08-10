package com.lightningkite.kiteui

import com.lightningkite.reactive.core.Release
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Deprecated("Safer alternative available that prevents memory leaks by respecting Element lifecycle", ReplaceWith("CoroutineScope.afterTimeout"))
public expect fun afterTimeout(milliseconds: Long, action: () -> Unit): () -> Unit

public inline fun CoroutineScope.afterTimeout(milliseconds: Long, crossinline action: () -> Unit): Release {
    val job = launch {
        delay(milliseconds)
        action()
    }
    return job::cancel
}
