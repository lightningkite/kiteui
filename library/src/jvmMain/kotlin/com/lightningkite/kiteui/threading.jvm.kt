package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineDispatcher

actual inline fun onMainThread(crossinline  action: () -> Unit): Unit = action()