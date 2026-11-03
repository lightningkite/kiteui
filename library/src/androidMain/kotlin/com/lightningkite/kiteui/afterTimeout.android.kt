package com.lightningkite.kiteui

import android.os.Handler
import android.os.Looper
import java.util.*
import kotlin.concurrent.schedule

private val handler = Handler(Looper.getMainLooper())

internal fun globalPost(action: ()->Unit) = handler.post(action)
public actual fun afterTimeout(milliseconds: Long, action: () -> Unit): () -> Unit {
    val runnable = Runnable(action)
    handler.postDelayed(runnable, milliseconds)
    return { handler.removeCallbacks(runnable) }
}