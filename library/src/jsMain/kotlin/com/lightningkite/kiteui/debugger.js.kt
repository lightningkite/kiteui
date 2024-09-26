package com.lightningkite.kiteui

var debug: Boolean = true
actual fun debugger() {
    if(debug) js("debugger;")
}

actual fun gc(): GCInfo {
    return GCInfo(-1L)
}
actual fun cleanImageCache() {
}
actual fun gcReport() {}

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

actual class WeakReference<T: Any> actual constructor(private val referred: T) {
    actual fun get(): T? = referred
}
//internal val anyIdentityHashCodeJsRef: (Any)->Int = js("""
//    (obj) => {
//        if (window.IDENTITY_HASH_CODE_SYMBOL === undefined) {
//            window.IDENTITY_HASH_CODE_SYMBOL = Symbol("KotlinIdentityHashCode");
//            window.lastIdentityHashCodeId = 1;
//        }
//        if (obj == null) return 0;
//        if (obj[window.IDENTITY_HASH_CODE_SYMBOL] === undefined) {
//            obj[window.IDENTITY_HASH_CODE_SYMBOL] = (window.lastIdentityHashCodeId = window.lastIdentityHashCodeId + 1);
//        }
//        return obj[window.IDENTITY_HASH_CODE_SYMBOL];
//    }
//""")
private var counter = 1
private var counterSymbol = js("Symbol(\"IDHC\")")
actual fun Any?.identityHashCode(): Int {
    if (this == null) return 0 else {
        val e = asDynamic()[counterSymbol]
        if (e != null) return e as Int
        val n = counter++
        asDynamic()[counterSymbol] = n
        return n
    }
}
