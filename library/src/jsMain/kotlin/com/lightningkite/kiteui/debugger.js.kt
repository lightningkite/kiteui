package com.lightningkite.kiteui

@InternalKiteUi
public var debug: Boolean = true
@InternalKiteUi
public actual fun debugger() {
    if(debug) js("debugger;")
}

@InternalKiteUi
public actual fun gc(): GCInfo {
    return GCInfo(-1L)
}
@InternalKiteUi
public actual fun cleanImageCache() {
}
public actual fun gcReport() {}

@InternalKiteUi
public actual fun assertMainThread() {
}

@InternalKiteUi
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
    public actual override fun tag(tag: String): Log = platform.tag(tag)
    public actual override fun log(vararg entries: Any?): Unit = platform.log(*entries)
    public actual override fun error(vararg entries: Any?): Unit = platform.error(*entries)
    public actual override fun info(vararg entries: Any?): Unit = platform.info(*entries)
    public actual override fun warn(vararg entries: Any?): Unit = platform.warn(*entries)
}
private class PlatformLog(val tag: String): Log {
    public override fun tag(tag: String): Log = PlatformLog(if(this.tag == "") tag else this.tag + "/" + tag)
    public override fun log(vararg entries: Any?) {
        console.log(tag, *entries)
    }

    public override fun error(vararg entries: Any?) {
        console.error(tag, *entries)
    }

    public override fun info(vararg entries: Any?) {
        console.info(tag, *entries)
    }

    public override fun warn(vararg entries: Any?) {
        console.warn(tag, *entries)
    }
}

private external interface WeakRef<T> {
    public fun deref(): T?
}

@InternalKiteUi
public actual class WeakReference<T: Any> public actual constructor(referred: T) {
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
@InternalKiteUi
public actual fun Any?.identityHashCode(): Int {
    if (this == null) return 0 else {
        val e = asDynamic()[counterSymbol]
        if (e != null) return e as Int
        val n = counter++
        asDynamic()[counterSymbol] = n
        return n
    }
}
