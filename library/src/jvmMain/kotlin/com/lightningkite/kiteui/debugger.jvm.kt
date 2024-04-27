package com.lightningkite.kiteui

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