package com.lightningkite.kiteui

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.asExecutor

val __mainHandler by lazy { Looper.getMainLooper().let(::Handler) }
actual inline fun onMainThread(crossinline  action: () -> Unit): Unit {
    if(Looper.myLooper() == Looper.getMainLooper()) action() else {
        __mainHandler.post { action() }
    }
}