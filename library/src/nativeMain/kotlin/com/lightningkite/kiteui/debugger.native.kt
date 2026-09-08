package com.lightningkite.kiteui

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode as idhc

public actual fun Throwable.printStackTrace2(): Unit = printStackTrace()
public actual object LogRoot: Log {
    private val platform = PlatformLog("")
    actual override val tag: String get() = platform.tag
    actual override fun tag(tag: String): Log = platform.tag(tag)
    actual override val level: LogLevel get() = platform.level
    actual override fun withLevel(level: LogLevel): Log = platform.withLevel(level)
    actual override fun log(vararg entries: Any?): Unit = platform.log(*entries)
    actual override fun info(vararg entries: Any?): Unit = platform.info(*entries)
    actual override fun warn(vararg entries: Any?): Unit = platform.warn(*entries)
    actual override fun error(vararg entries: Any?): Unit = platform.error(*entries)
}
private class PlatformLog(override val tag: String, override val level: LogLevel = LogLevel.LOG): Log {
    override fun tag(tag: String): Log = PlatformLog(if(this.tag == "") tag else this.tag + "/" + tag, level)
    override fun withLevel(level: LogLevel): Log = PlatformLog(tag, level)
    override fun log(vararg entries: Any?) {
        if (level < LogLevel.LOG) return
        println("$tag: " + entries.joinToString(" "))
    }

    override fun info(vararg entries: Any?) {
        if (level < LogLevel.INFO) return
        println("$tag: " + entries.joinToString(" "))
    }

    override fun warn(vararg entries: Any?) {
        if (level < LogLevel.WARN) return
        println("$tag: " + entries.joinToString(" "))
    }

    override fun error(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }
}

@OptIn(ExperimentalNativeApi::class)
public actual typealias WeakReference<T> = kotlin.native.ref.WeakReference<T>

@OptIn(ExperimentalNativeApi::class)
public actual fun Any?.identityHashCode(): Int = idhc()
