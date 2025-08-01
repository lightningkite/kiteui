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
    public actual override fun log(vararg entries: Any?): Unit = platform.log(*entries)
    public actual override fun error(vararg entries: Any?): Unit = platform.error(*entries)
    public actual override fun info(vararg entries: Any?): Unit = platform.info(*entries)
    public actual override fun warn(vararg entries: Any?): Unit = platform.warn(*entries)
}
private class PlatformConsole(val tag: String): Console {
    public override fun tag(tag: String): Console = PlatformConsole(tag)
    public override fun log(vararg entries: Any?) {
        Log.d(tag, entries.joinToString(" "))
    }

    public override fun error(vararg entries: Any?) {
        Log.e(tag, entries.joinToString(" "))
    }

    public override fun info(vararg entries: Any?) {
        Log.i(tag, entries.joinToString(" "))
    }

    public override fun warn(vararg entries: Any?) {
        Log.w(tag, entries.joinToString(" "))
    }
}

public actual fun Any?.identityHashCode(): Int = System.identityHashCode(this)
