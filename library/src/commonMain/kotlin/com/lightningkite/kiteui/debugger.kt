package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView

expect fun debugger(): Unit
data class GCInfo(val usage: Long)
expect fun gc(): GCInfo
expect fun assertMainThread()

expect fun Throwable.printStackTrace2()
var Throwable_report: (Throwable, String) -> Unit = { e, _ -> e.printStackTrace2() }
fun Throwable.report(context: String = "") = Throwable_report(this, context)

var viewDebugTarget: NView? = null

interface Console {
    fun tag(tag: String): Console
    fun log(vararg entries: Any?)
    fun error(vararg entries: Any?)
    fun info(vararg entries: Any?)
    fun warn(vararg entries: Any?)
}
expect object ConsoleRoot: Console