package com.lightningkite.kiteui

var debug: Boolean = true
actual fun debugger() {
    if(debug) js("debugger;")
}

actual fun gc(): GCInfo {
    return GCInfo(-1L)
}

actual fun assertMainThread() {
}

actual fun Throwable.printStackTrace2() {
    val stack = this.asDynamic().stack
    if (stack is String) {
        val error = js("Error()")
        error.name = this.toString().substringBefore(':')
        error.message = this.message?.substringAfter(':')
        error.stack = stack
        console.error(error)
    } else {
        console.log(this)
    }
}

actual object ConsoleRoot: Console by PlatformConsole("MyApp")
private class PlatformConsole(val tag: String): Console {
    override fun tag(tag: String): Console = PlatformConsole(tag)
    override fun log(vararg entries: Any?) {
        console.log(tag, *entries)
    }

    override fun error(vararg entries: Any?) {
        console.error(tag, *entries)
    }

    override fun info(vararg entries: Any?) {
        console.info(tag, *entries)
    }

    override fun warn(vararg entries: Any?) {
        console.warn(tag, *entries)
    }
}