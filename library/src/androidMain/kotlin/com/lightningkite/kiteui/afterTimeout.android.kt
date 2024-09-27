package com.lightningkite.kiteui

import android.os.Handler
import android.os.Looper
import java.util.*
import kotlin.concurrent.schedule

private val handler = Handler(Looper.getMainLooper())

internal fun globalPost(action: ()->Unit) = handler.post(action)
actual fun afterTimeout(milliseconds: Long, action: () -> Unit): () -> Unit {
    handler.postDelayed(action, milliseconds)
    return { handler.removeCallbacks(action) }
}