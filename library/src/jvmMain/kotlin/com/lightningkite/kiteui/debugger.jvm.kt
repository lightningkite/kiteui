package com.lightningkite.kiteui

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
}
public actual fun gcReport() {}

public actual typealias WeakReference<T> = WeakReference<T>

public actual fun assertMainThread() {
}

public actual fun Throwable.printStackTrace2() = printStackTrace()

actual object LogRoot: Log {
    private val platform = PlatformLog("")
    public actual override fun tag(tag: String): Log = platform.tag(tag)
    public actual override fun log(vararg entries: Any?) = platform.log(*entries)
    public actual override fun error(vararg entries: Any?) = platform.error(*entries)
    public actual override fun info(vararg entries: Any?) = platform.info(*entries)
    public actual override fun warn(vararg entries: Any?) = platform.warn(*entries)
}
private class PlatformLog(val tag: String): Log {
    override fun tag(tag: String): Log = PlatformLog(if(this.tag == "") tag else this.tag + "/" + tag)
    override fun log(vararg entries: Any?) {
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

public actual fun Any?.identityHashCode(): Int = System.identityHashCode(this)
