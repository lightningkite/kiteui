package com.lightningkite.kiteui

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlin.native.identityHashCode as idhc

@InternalKiteUi
public actual fun Throwable.printStackTrace2(): Unit = printStackTrace()
public actual object LogRoot: Log {
    private val platform = PlatformLog("")
    public actual override fun tag(tag: String): Log = platform.tag(tag)
    public actual override fun log(vararg entries: Any?): Unit = platform.log(*entries)
    public actual override fun error(vararg entries: Any?): Unit = platform.error(*entries)
    public actual override fun info(vararg entries: Any?): Unit = platform.info(*entries)
    public actual override fun warn(vararg entries: Any?): Unit = platform.warn(*entries)
}
private class PlatformLog(val tag: String): Log {
    public override fun tag(tag: String): Log = PlatformLog(if(this.tag == "") tag else this.tag + "/" + tag)
    public override fun log(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }

    public override fun error(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }

    public override fun info(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }

    public override fun warn(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }
}

@OptIn(ExperimentalNativeApi::class)
@InternalKiteUi
public actual typealias WeakReference<T> = WeakReference<T>

@OptIn(ExperimentalNativeApi::class)
public actual fun Any?.identityHashCode(): Int = idhc()
