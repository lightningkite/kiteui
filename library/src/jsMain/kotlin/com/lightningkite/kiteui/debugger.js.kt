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
    printStackTrace()
//    val stack = this.asDynamic().stack
//    if (stack is String) {
//        val error = js("Error()")
//        error.name = this.toString().substringBefore(':')
//        error.message = this.message?.substringAfter(':')
//        error.stack = stack
//        console.error(error)
//    } else {
//        console.log(this)
//    }
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

private external interface WeakRef<T> {
    fun deref(): T?
}

actual class WeakReference<T: Any> actual constructor(referred: T) {
    actual fun get(): T? = native?.deref()
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private val native: WeakRef<T>? = try {
        js("WeakRef(referred)") as? WeakRef<T>
    } catch(e: dynamic) {
        null
    }
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
