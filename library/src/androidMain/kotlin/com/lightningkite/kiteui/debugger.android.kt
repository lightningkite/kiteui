package com.lightningkite.kiteui

import android.util.Log
import java.lang.ref.WeakReference

actual fun debugger() {

}

actual fun gc(): GCInfo {
    return Runtime.getRuntime().run {
        gc()
        GCInfo(totalMemory() - freeMemory())
    }
}
actual fun cleanImageCache() {
    // TODO
}

actual fun gcReport() {}

actual typealias WeakReference<T> = WeakReference<T>

actual fun assertMainThread() {
}

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
        Log.d(tag, entries.joinToString(" "))
    }

    override fun error(vararg entries: Any?) {
        Log.e(tag, entries.joinToString(" "))
    }

    override fun info(vararg entries: Any?) {
        Log.i(tag, entries.joinToString(" "))
    }

    override fun warn(vararg entries: Any?) {
        Log.w(tag, entries.joinToString(" "))
    }
}

actual fun Any?.identityHashCode(): Int = System.identityHashCode(this)
