package com.lightningkite.kiteui

import android.util.Log
import java.lang.ref.WeakReference

public actual fun debugger() {

}

public actual fun gc(): GCInfo {
    return Runtime.getRuntime().run {
        gc()
        GCInfo(totalMemory() - freeMemory())
    }
}
public actual fun cleanImageCache() {
    // TODO
}

public actual fun gcReport() {}

public actual typealias WeakReference<T> = WeakReference<T>

public actual fun assertMainThread() {
}

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

public actual fun Any?.identityHashCode(): Int = System.identityHashCode(this)
