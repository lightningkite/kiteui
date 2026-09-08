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

@Suppress("DEPRECATION")
public fun WeakReference<*>.checkLeakAfterDelay(milliseconds: Long) {
    afterTimeout(milliseconds) {
        gcIfNotVeryRecent()
        get()?.let {
            leaks.add(this)
            recheckLeakAfterDelay(milliseconds)
        }
    }
}

@Suppress("DEPRECATION")
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

@Suppress("DEPRECATION")
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

@Suppress("DEPRECATION")
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
public typealias ConsoleRoot = Log.Default

public enum class LogLevel {
    /** Something failed and needs attention; shown even by the most restrictive filters. */
    ERROR,
    /** Something is off but the app can keep going. */
    WARN,
    /** Notable, expected events worth surfacing to someone watching the log. */
    INFO,
    /** Routine or high-volume detail, useful for debugging but noisy in normal operation. */
    LOG,
}

/** Observes log calls without owning the delegation chain. Is only called if the log's current level would actually log.  */
public fun interface LogInterceptor {
    public fun intercept(level: LogLevel, tag: String, entries: Array<out Any?>)
}

public interface Log {
    public val tag: String
    public fun tag(tag: String): Log

    public val level: LogLevel
    public fun withLevel(level: LogLevel): Log

    public fun log(vararg entries: Any?)
    public fun info(vararg entries: Any?)
    public fun warn(vararg entries: Any?)
    public fun error(vararg entries: Any?)

    public companion object Default : Log by InterceptedLog(LogRoot) {
        /** Interceptors that observe all [Log] calls. Each receives every call; [LogRoot] is always called regardless. */
        public val interceptors: MutableList<LogInterceptor> = mutableListOf()
    }
}

public fun Log.output(level: LogLevel, vararg entries: Any?): Unit = when (level) {
    LogLevel.ERROR -> error(*entries)
    LogLevel.WARN -> warn(*entries)
    LogLevel.INFO -> info(*entries)
    LogLevel.LOG -> log(*entries)
}

public fun Log.atLevel(level: LogLevel): Boolean = this.level >= level

public fun Log(tag: String, level: LogLevel = LogLevel.LOG): Log = Log.tag(tag).withLevel(level)

/** Drops LOG. Composes with other `*OrAbove` filters by narrowing only, never widening a stricter one. */
public fun Log.infoOrAbove(): Log = withLevel(minOf(level, LogLevel.INFO))

/** Drops LOG and INFO. Composes with other `*OrAbove` filters by narrowing only, never widening a stricter one. */
public fun Log.warnOrAbove(): Log = withLevel(minOf(level, LogLevel.WARN))



private class InterceptedLog(private val wraps: Log) : Log {
    override val tag: String get() = wraps.tag
    override fun tag(tag: String): Log = InterceptedLog(wraps.tag(tag))

    override val level: LogLevel get() = wraps.level
    override fun withLevel(level: LogLevel): Log = InterceptedLog(wraps.withLevel(level))

    private fun intercept(level: LogLevel, entries: Array<out Any?>) {
        for (i in Log.interceptors) i.intercept(level, wraps.tag, entries)
    }

    override fun log(vararg entries: Any?) {
        if (wraps.level < LogLevel.LOG) return
        intercept(LogLevel.LOG, entries)
        wraps.log(*entries)
    }
    override fun info(vararg entries: Any?) {
        if (wraps.level < LogLevel.INFO) return
        intercept(LogLevel.INFO, entries)
        wraps.info(*entries)
    }
    override fun warn(vararg entries: Any?) {
        if (wraps.level < LogLevel.WARN) return
        intercept(LogLevel.WARN, entries)
        wraps.warn(*entries)
    }
    override fun error(vararg entries: Any?) {
        intercept(LogLevel.ERROR, entries)
        wraps.error(*entries)
    }
}

public expect object LogRoot : Log {
    override val tag: String
    override fun tag(tag: String): Log
    override val level: LogLevel
    override fun withLevel(level: LogLevel): Log
    override fun log(vararg entries: Any?)
    override fun info(vararg entries: Any?)
    override fun warn(vararg entries: Any?)
    override fun error(vararg entries: Any?)
}