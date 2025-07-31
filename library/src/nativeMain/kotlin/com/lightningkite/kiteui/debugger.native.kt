package com.lightningkite.kiteui

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode as idhc

public actual fun Throwable.printStackTrace2() = printStackTrace()
public actual object ConsoleRoot: Console {
    private val platform = PlatformConsole("MyApp")
    public actual override fun tag(tag: String): Console = platform.tag(tag)
    public actual override fun log(vararg entries: Any?) = platform.log(*entries)
    public actual override fun error(vararg entries: Any?) = platform.error(*entries)
    public actual override fun info(vararg entries: Any?) = platform.info(*entries)
    public actual override fun warn(vararg entries: Any?) = platform.warn(*entries)
}
private class PlatformConsole(val tag: String): Console {
    override fun tag(tag: String): Console = PlatformConsole(tag)
    override fun log(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }

    override fun error(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }

    override fun info(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }

    override fun warn(vararg entries: Any?) {
        println("$tag: " + entries.joinToString(" "))
    }
}

@OptIn(ExperimentalNativeApi::class)
public actual typealias WeakReference<T> = kotlin.native.ref.WeakReference<T>

@OptIn(ExperimentalNativeApi::class)
public actual fun Any?.identityHashCode(): Int = idhc()
