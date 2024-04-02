package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView

expect fun debugger(): Unit
data class GCInfo(val usage: Long)
expect fun gc(): GCInfo
expect fun assertMainThread()

expect fun Throwable.printStackTrace2()

var viewDebugTarget: NView? = null