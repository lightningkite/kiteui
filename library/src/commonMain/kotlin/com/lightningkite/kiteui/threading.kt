package com.lightningkite.kiteui


expect inline fun onMainThread(crossinline action: () -> Unit): Unit