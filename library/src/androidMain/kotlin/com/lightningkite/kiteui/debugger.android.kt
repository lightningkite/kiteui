package com.lightningkite.kiteui

import android.util.Log
import java.lang.ref.WeakReference

@InternalKiteUi
public actual fun debugger() {

}

@InternalKiteUi
public actual fun gc(): GCInfo {
    return Runtime.getRuntime().run {
        gc()
        GCInfo(totalMemory() - freeMemory())
    }
}
@InternalKiteUi
public actual fun cleanImageCache() {
    // TODO
}

public actual fun gcReport() {}

@InternalKiteUi
public actual typealias WeakReference<T> = WeakReference<T>

@InternalKiteUi
public actual fun assertMainThread() {
}

@InternalKiteUi
public actual fun Throwable.printStackTrace2():Unit = printStackTrace()


public actual object LogRoot: com.lightningkite.kiteui.Log {
    private val platform = PlatformLog("")
    public actual override fun tag(tag: String): com.lightningkite.kiteui.Log = platform.tag(tag)
    public actual override fun log(vararg entries: Any?): Unit = platform.log(*entries)
    public actual override fun error(vararg entries: Any?): Unit = platform.error(*entries)
    public actual override fun info(vararg entries: Any?): Unit = platform.info(*entries)
    public actual override fun warn(vararg entries: Any?): Unit = platform.warn(*entries)
}
 private class PlatformLog(val tag: String): com.lightningkite.kiteui.Log {
    public override fun tag(tag: String): com.lightningkite.kiteui.Log = PlatformLog(if(this.tag == "") tag else this.tag + "/" + tag)
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

@InternalKiteUi
public actual fun Any?.identityHashCode(): Int = System.identityHashCode(this)
