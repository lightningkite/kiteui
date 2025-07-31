package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Deprecated("Safer alternative available that prevents memory leaks by respecting RView lifecycle", ReplaceWith("CoroutineScope.afterTimeout"))
public expect fun afterTimeout(milliseconds: Long, action: ()->Unit): ()->Unit

fun CoroutineScope.afterTimeout(milliseconds: Long, action: ()->Unit) = launch {
    delay(milliseconds)
    action()
}.let { { it.cancel() } }
