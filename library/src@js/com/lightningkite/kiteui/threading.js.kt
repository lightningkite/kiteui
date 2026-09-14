package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

public actual inline fun onMainThread(crossinline  action: () -> Unit): Unit = action()