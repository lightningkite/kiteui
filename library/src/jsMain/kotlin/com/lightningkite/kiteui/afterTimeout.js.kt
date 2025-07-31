package com.lightningkite.kiteui

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

public actual inline fun afterTimeout(milliseconds: Long, crossinline action: () -> Unit): () -> Unit {
    val handle = window.setTimeout({ ->
        action()
    }, milliseconds.toInt())
    return {
        window.clearTimeout(handle)
    }
}

suspend fun <T> Promise<T>.await(): T = suspendCancellableCoroutine { cont ->
    then(
        onFulfilled = {
            cont.resume(it)
        },
        onRejected = {
            cont.resumeWithException(it)
        }
    )
}