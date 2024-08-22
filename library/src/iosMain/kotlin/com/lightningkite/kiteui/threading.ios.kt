package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual inline fun onMainThread(crossinline  action: () -> Unit): Unit = if(NSThread.isMainThread) action() else {
    dispatch_async(dispatch_get_main_queue()) { action() }
}