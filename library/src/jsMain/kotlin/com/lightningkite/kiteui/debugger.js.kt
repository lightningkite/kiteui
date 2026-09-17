package com.lightningkite.kiteui

public var debug: Boolean = true
public actual fun debugger() {
    if(debug) js("debugger;")
}

public actual fun gc(): GCInfo {
    return GCInfo(-1L)
}
public actual fun cleanImageCache() {
}
public actual fun gcReport() {}

public actual fun assertMainThread() {
}

public actual fun Throwable.printStackTrace2() {
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

public actual object LogRoot: Log {
    private val platform = PlatformLog("")
    actual override val tag: String get() = platform.tag
    actual override fun tag(tag: String): Log = platform.tag(tag)
    actual override val level: LogLevel get() = platform.level
    actual override fun withLevel(level: LogLevel): Log = platform.withLevel(level)
    actual override fun log(vararg entries: Any?): Unit = platform.log(*entries)
    actual override fun info(vararg entries: Any?): Unit = platform.info(*entries)
    actual override fun warn(vararg entries: Any?): Unit = platform.warn(*entries)
    actual override fun error(vararg entries: Any?): Unit = platform.error(*entries)
}
private class PlatformLog(override val tag: String, override val level: LogLevel = LogLevel.LOG): Log {
    override fun tag(tag: String): Log = PlatformLog(if(this.tag == "") tag else this.tag + "/" + tag, level)
    override fun withLevel(level: LogLevel): Log = PlatformLog(tag, level)
    override fun log(vararg entries: Any?) {
        if (level < LogLevel.LOG) return
        console.log(tag, *entries)
    }

    override fun info(vararg entries: Any?) {
        if (level < LogLevel.INFO) return
        console.info(tag, *entries)
    }

    override fun warn(vararg entries: Any?) {
        if (level < LogLevel.WARN) return
        console.warn(tag, *entries)
    }

    override fun error(vararg entries: Any?) {
        console.error(tag, *entries)
    }
}

private external interface WeakRef<T> {
    fun deref(): T?
}

public actual class WeakReference<T: Any> actual constructor(referred: T) {
    public actual fun get(): T? = native?.deref()
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
public actual fun Any?.identityHashCode(): Int {
    if (this == null) return 0 else {
        val e = asDynamic()[counterSymbol]
        if (e != null) return e as Int
        val n = counter++
        asDynamic()[counterSymbol] = n
        return n
    }
}
