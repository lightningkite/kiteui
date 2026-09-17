package com.lightningkite.kiteui

public expect object PlatformStorage {
    public fun get(key: String): String?
    public fun set(key: String, value: String)
    public fun remove(key: String)
}
