package com.lightningkite.kiteui

import android.util.Log

actual fun debugger() {

}

actual fun gc(): GCInfo {
    return Runtime.getRuntime().run {
        gc()
        GCInfo(totalMemory() - freeMemory())
    }
}

actual fun assertMainThread() {
}

actual fun Throwable.printStackTrace2() = printStackTrace()


actual object ConsoleRoot: Console by PlatformConsole("MyApp")
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