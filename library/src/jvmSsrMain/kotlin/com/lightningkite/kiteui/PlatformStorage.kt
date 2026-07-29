package com.lightningkite.kiteui


public actual object PlatformStorage {
    public actual fun get(key: String): String? = null
    public actual fun set(key: String, value: String): Unit {}
    public actual fun remove(key: String): Unit {}
}
