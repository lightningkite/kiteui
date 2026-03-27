package com.lightningkite.kiteui

var debugMode: Boolean = false
expect fun debugger(): Unit
data class GCInfo(val usage: Long)
expect fun gc(): GCInfo
expect fun cleanImageCache()
expect fun gcReport()
expect class WeakReference<T: Any>(referred: T) {
    fun get(): T?
}
val leaks = ArrayList<WeakReference<*>>()
private var lastGc = clockMillis()
private var lastGcReport = clockMillis()
private val leakLog = LogRoot.tag("RViewLeaks")
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
fun WeakReference<*>.checkLeakAfterDelay(milliseconds: Long, name: String) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        get()?.let {
            println("Leaked $name")
            leaks.add(this)
            recheckLeakAfterDelay(milliseconds, name)
        }
    }
}
fun WeakReference<*>.recheckLeakAfterDelay(milliseconds: Long, name: String) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        if (get() == null) {
            println("Leak $name cleaned up")
            leaks.remove(this)
        } else {
            recheckLeakAfterDelay(milliseconds, name)
        }
    }
}
expect fun assertMainThread()

expect fun Throwable.printStackTrace2()
var Throwable_report: (Throwable, String) -> Unit = { e, _ -> e.printStackTrace2() }
fun Throwable.report(context: String = "") = Throwable_report(this, context)

expect fun Any?.identityHashCode(): Int

var viewDebugTarget: RView? = null

inline fun RViewHelper.debugPrint(get: ()->String) {
    if(debugMode && viewDebugTarget == this)
        Log.tag("viewDebugTarget").info(get())
}
inline fun RView.debugPrint(get: ()->String) {
    if(debugMode && viewDebugTarget == this)
        Log.tag("viewDebugTarget").info(get())
}

@Deprecated("Update to 'Log'", ReplaceWith("Log", "com.lightningkite.kiteui.Log"))
typealias Console = Log
@Deprecated("Update to 'Log'", ReplaceWith("Log", "com.lightningkite.kiteui.Log"))
typealias ConsoleRoot = Log.Companion

interface Log {
    companion object: Log {
        override fun tag(tag: String): Log = (logInterceptor ?: LogRoot).tag(tag)
        override fun log(vararg entries: Any?) = (logInterceptor ?: LogRoot).log(*entries)
        override fun error(vararg entries: Any?) = (logInterceptor ?: LogRoot).error(*entries)
        override fun info(vararg entries: Any?) = (logInterceptor ?: LogRoot).info(*entries)
        override fun warn(vararg entries: Any?) = (logInterceptor ?: LogRoot).warn(*entries)
    }
    fun tag(tag: String): Log
    fun log(vararg entries: Any?)
    fun error(vararg entries: Any?)
    fun info(vararg entries: Any?)
    fun warn(vararg entries: Any?)
}

/**
 * Optional log interceptor. When set, all `Log.Companion` calls are forwarded through this
 * instead of directly to [LogRoot]. Used by the AI driver to buffer log output.
 */
var logInterceptor: Log? = null
fun Log.infoOrAbove(): Log = object : Log by this {
    override fun log(vararg entries: Any?) {}
}
fun Log.warnOrAbove(): Log = object : Log by this {
    override fun log(vararg entries: Any?) {}
    override fun info(vararg entries: Any?) {}
}
expect object LogRoot: Log {
    override fun tag(tag: String): Log
    override fun log(vararg entries: Any?)
    override fun error(vararg entries: Any?)
    override fun info(vararg entries: Any?)
    override fun warn(vararg entries: Any?)
}