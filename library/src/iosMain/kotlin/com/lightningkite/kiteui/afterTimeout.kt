package com.lightningkite.kiteui

import platform.darwin.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual inline fun afterTimeout(milliseconds: Long, crossinline action: () -> Unit): () -> Unit {
    var stillRun: Boolean = true
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, milliseconds * NSEC_PER_MSEC.toLong()), dispatch_get_main_queue()) {
        if(!stillRun) return@dispatch_after
        action()
    }
    return { stillRun = false }
}
