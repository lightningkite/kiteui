package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.NativeElement

var debugMode: Boolean = false
expect fun debugger(): Unit
data class GCInfo(val usage: Long)

expect fun gc(): GCInfo
expect fun cleanImageCache()
expect fun gcReport()
expect class WeakReference<T : Any>(referred: T) {
    fun get(): T?
}

val leaks = ArrayList<WeakReference<*>>()
private var lastGc = clockMillis()
private var lastGcReport = clockMillis()
private val leakLog = LogRoot.tag("ElementLeaks")
private fun gcIfNotVeryRecent() {
    if (clockMillis() - lastGc > 100.0) {
        gc()
        lastGc = clockMillis()
    }
    if (clockMillis() - lastGcReport > 1000.0) {
        lastGcReport = clockMillis()
        if (leaks.isNotEmpty()) {
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

var viewDebugTarget: Element? = null

inline fun Element.debugPrint(get: () -> String) {
    if (debugMode && viewDebugTarget == this)
        Log.tag("viewDebugTarget").info(get())
}

@Deprecated("Update to 'Log'", ReplaceWith("Log", "com.lightningkite.kiteui.Log"))
typealias Console = Log

@Deprecated("Update to 'Log'", ReplaceWith("Log", "com.lightningkite.kiteui.Log"))
typealias ConsoleRoot = Log.Companion

enum class LogLevel { LOG, INFO, WARN, ERROR }

/** Observes log calls without owning the delegation chain. [LogRoot] is always called regardless. */
fun interface LogInterceptor {
    fun intercept(level: LogLevel, tag: String, entries: Array<out Any?>)
}

/** Interceptors that observe all [Log] calls. Each receives every call; [LogRoot] is always called regardless. */
val logInterceptors: MutableList<LogInterceptor> = mutableListOf()

interface Log {
    companion object: Log {
        override fun tag(tag: String): Log = InterceptedTaggedLog(tag)
        override fun log(vararg entries: Any?) {
            for (i in logInterceptors) i.intercept(LogLevel.LOG, "", entries)
            LogRoot.log(*entries)
        }
        override fun error(vararg entries: Any?) {
            for (i in logInterceptors) i.intercept(LogLevel.ERROR, "", entries)
            LogRoot.error(*entries)
        }
        override fun info(vararg entries: Any?) {
            for (i in logInterceptors) i.intercept(LogLevel.INFO, "", entries)
            LogRoot.info(*entries)
        }
        override fun warn(vararg entries: Any?) {
            for (i in logInterceptors) i.intercept(LogLevel.WARN, "", entries)
            LogRoot.warn(*entries)
        }
    }

    fun tag(tag: String): Log
    fun log(vararg entries: Any?)
    fun error(vararg entries: Any?)
    fun info(vararg entries: Any?)
    fun warn(vararg entries: Any?)
}

private class InterceptedTaggedLog(val tag: String) : Log {
    private val rootTagged = LogRoot.tag(tag)
    override fun tag(tag: String): Log = InterceptedTaggedLog("${this.tag}/$tag")
    override fun log(vararg entries: Any?) {
        for (i in logInterceptors) i.intercept(LogLevel.LOG, tag, entries)
        rootTagged.log(*entries)
    }
    override fun error(vararg entries: Any?) {
        for (i in logInterceptors) i.intercept(LogLevel.ERROR, tag, entries)
        rootTagged.error(*entries)
    }
    override fun info(vararg entries: Any?) {
        for (i in logInterceptors) i.intercept(LogLevel.INFO, tag, entries)
        rootTagged.info(*entries)
    }
    override fun warn(vararg entries: Any?) {
        for (i in logInterceptors) i.intercept(LogLevel.WARN, tag, entries)
        rootTagged.warn(*entries)
    }
}
fun Log.infoOrAbove(): Log = object : Log by this {
    override fun log(vararg entries: Any?) {}
}

fun Log.warnOrAbove(): Log = object : Log by this {
    override fun log(vararg entries: Any?) {}
    override fun info(vararg entries: Any?) {}
}

expect object LogRoot : Log {
    override fun tag(tag: String): Log
    override fun log(vararg entries: Any?)
    override fun error(vararg entries: Any?)
    override fun info(vararg entries: Any?)
    override fun warn(vararg entries: Any?)
}