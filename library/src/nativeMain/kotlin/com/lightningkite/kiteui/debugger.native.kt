package com.lightningkite.kiteui

import kotlin.experimental.ExperimentalNativeApi

actual fun Throwable.printStackTrace2() = printStackTrace()
actual object ConsoleRoot: Console {
    private val platform = PlatformConsole("MyApp")
    actual override fun tag(tag: String): Console = platform.tag(tag)
    actual override fun log(vararg entries: Any?) = platform.log(*entries)
    actual override fun error(vararg entries: Any?) = platform.error(*entries)
    actual override fun info(vararg entries: Any?) = platform.info(*entries)
    actual override fun warn(vararg entries: Any?) = platform.warn(*entries)
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
actual typealias WeakReference<T> = kotlin.native.ref.WeakReference<T>