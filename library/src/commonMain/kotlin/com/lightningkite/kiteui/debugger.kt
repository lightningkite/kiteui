package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.Element

public var debugMode: Boolean = false

@Deprecated("Use Element.Debugger.debugTarget", ReplaceWith("Element.Debugger.debugTarget"))
public var viewDebugTarget: Element? by Element.Debugger::debugTarget

public expect fun debugger(): Unit
public data class GCInfo(val usage: Long)

public expect fun gc(): GCInfo
public expect fun cleanImageCache()
public expect fun gcReport()
public expect class WeakReference<T : Any>(referred: T) {
    public fun get(): T?
}

public val leaks: MutableList<WeakReference<*>> = ArrayList<WeakReference<*>>()
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

public fun WeakReference<*>.checkLeakAfterDelay(milliseconds: Long) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        get()?.let {
            leaks.add(this)
            recheckLeakAfterDelay(milliseconds)
        }
    }
}

public fun WeakReference<*>.recheckLeakAfterDelay(milliseconds: Long) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        if (get() == null) {
            leaks.remove(this)
        } else {
            recheckLeakAfterDelay(milliseconds)
        }
    }
}

public fun WeakReference<*>.checkLeakAfterDelay(milliseconds: Long, name: String) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        get()?.let {
            println("Leaked $name")
            leaks.add(this)
            recheckLeakAfterDelay(milliseconds, name)
        }
    }
}

public fun WeakReference<*>.recheckLeakAfterDelay(milliseconds: Long, name: String) {
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

public expect fun assertMainThread()

public expect fun Throwable.printStackTrace2()
public var Throwable_report: (Throwable, String) -> Unit = { e, _ -> e.printStackTrace2() }
public fun Throwable.report(context: String = ""): Unit = Throwable_report(this, context)

public expect fun Any?.identityHashCode(): Int

public inline fun Element.debugPrint(get: () -> String) {
    if (debugMode && Element.Debugger.debugTarget == this)
        Log.tag("viewDebugTarget").info(get())
}

@Deprecated("Update to 'Log'", ReplaceWith("Log", "com.lightningkite.kiteui.Log"))
public typealias Console = Log

@Deprecated("Update to 'Log'", ReplaceWith("Log", "com.lightningkite.kiteui.Log"))
public typealias ConsoleRoot = Log.Companion

public enum class LogLevel { LOG, INFO, WARN, ERROR }

/** Observes log calls without owning the delegation chain. [LogRoot] is always called regardless. */
public fun interface LogInterceptor {
    public fun intercept(level: LogLevel, tag: String, entries: Array<out Any?>)
}

public interface Log {
    public companion object: Log {
        /** Interceptors that observe all [Log] calls. Each receives every call; [LogRoot] is always called regardless. */
        public val interceptors: MutableList<LogInterceptor> = mutableListOf()

        override fun tag(tag: String): Log = InterceptedTaggedLog(tag)

        override fun log(vararg entries: Any?) {
            for (i in interceptors) i.intercept(LogLevel.LOG, "", entries)
            LogRoot.log(*entries)
        }
        override fun error(vararg entries: Any?) {
            for (i in interceptors) i.intercept(LogLevel.ERROR, "", entries)
            LogRoot.error(*entries)
        }
        override fun info(vararg entries: Any?) {
            for (i in interceptors) i.intercept(LogLevel.INFO, "", entries)
            LogRoot.info(*entries)
        }
        override fun warn(vararg entries: Any?) {
            for (i in interceptors) i.intercept(LogLevel.WARN, "", entries)
            LogRoot.warn(*entries)
        }
    }

    public fun tag(tag: String): Log
    public fun log(vararg entries: Any?)
    public fun error(vararg entries: Any?)
    public fun info(vararg entries: Any?)
    public fun warn(vararg entries: Any?)
}

private class InterceptedTaggedLog(val tag: String) : Log {
    private val rootTagged = LogRoot.tag(tag)
    override fun tag(tag: String): Log = InterceptedTaggedLog("${this.tag}/$tag")
    override fun log(vararg entries: Any?) {
        for (i in Log.interceptors) i.intercept(LogLevel.LOG, tag, entries)
        rootTagged.log(*entries)
    }
    override fun error(vararg entries: Any?) {
        for (i in Log.interceptors) i.intercept(LogLevel.ERROR, tag, entries)
        rootTagged.error(*entries)
    }
    override fun info(vararg entries: Any?) {
        for (i in Log.interceptors) i.intercept(LogLevel.INFO, tag, entries)
        rootTagged.info(*entries)
    }
    override fun warn(vararg entries: Any?) {
        for (i in Log.interceptors) i.intercept(LogLevel.WARN, tag, entries)
        rootTagged.warn(*entries)
    }
}

public fun Log.infoOrAbove(): Log = object : Log by this {
    override fun log(vararg entries: Any?) {}
}

public fun Log.warnOrAbove(): Log = object : Log by this {
    override fun log(vararg entries: Any?) {}
    override fun info(vararg entries: Any?) {}
}

public expect object LogRoot : Log {
    override fun tag(tag: String): Log
    override fun log(vararg entries: Any?)
    override fun error(vararg entries: Any?)
    override fun info(vararg entries: Any?)
    override fun warn(vararg entries: Any?)
}