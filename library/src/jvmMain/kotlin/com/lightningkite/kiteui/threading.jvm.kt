package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineDispatcher

@InternalKiteUi
public actual inline fun onMainThread(crossinline  action: () -> Unit): Unit = action()