package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RView

var debugMode: Boolean = false
public expect fun debugger(): Unit
data class GCInfo(val usage: Long)
public expect fun gc(): GCInfo
public expect fun cleanImageCache()
public expect fun gcReport()
public expect class WeakReference<T: Any>(referred: T) {
    fun get(): T?
}
val leaks = ArrayList<WeakReference<*>>()
private var lastGc = clockMillis()
private var lastGcReport = clockMillis()
private val leakLog = ConsoleRoot.tag("RViewLeaks")
private fun gcIfNotVeryRecent() {
    if(clockMillis() - lastGc > 100.0) {
        gc()
        lastGc = clockMillis()
    }
    if(clockMillis() - lastGcReport > 1000.0) {
        lastGcReport = clockMillis()
        if(leaks.isNotEmpty()) {
            leakLog.log("WARNING: ${leaks.size} leaks...")
            leaks.groupingBy { it.get()?.let { it::class } }.eachCount().forEach {
                leakLog.log("  Leaked ${it.value} of ${it.key}")
            }
        }
    }
}
fun WeakReference<*>.checkLeakAfterDelay(milliseconds: Long) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        get()?.let {
            leaks.add(this)
            recheckLeakAfterDelay(milliseconds)
        }
    }
}
fun WeakReference<*>.recheckLeakAfterDelay(milliseconds: Long) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        if (get() == null) {
            leaks.remove(this)
        } else {
            recheckLeakAfterDelay(milliseconds)
        }
    }
}
public expect fun assertMainThread()

public expect fun Throwable.printStackTrace2()
var Throwable_report: (Throwable, String) -> Unit = { e, _ -> e.printStackTrace2() }
fun Throwable.report(context: String = "") = Throwable_report(this, context)

public expect fun Any?.identityHashCode(): Int

var viewDebugTarget: RView? = null

interface Console {
    fun tag(tag: String): Console
    fun log(vararg entries: Any?)
    fun error(vararg entries: Any?)
    fun info(vararg entries: Any?)
    fun warn(vararg entries: Any?)
}
fun Console.infoOrAbove(): Console = object : Console by this {
    override fun log(vararg entries: Any?) {}
}
fun Console.warnOrAbove(): Console = object : Console by this {
    override fun log(vararg entries: Any?) {}
    override fun info(vararg entries: Any?) {}
}
public expect object ConsoleRoot: Console {
    override fun tag(tag: String): Console
    override fun log(vararg entries: Any?)
    override fun error(vararg entries: Any?)
    override fun info(vararg entries: Any?)
    override fun warn(vararg entries: Any?)
}