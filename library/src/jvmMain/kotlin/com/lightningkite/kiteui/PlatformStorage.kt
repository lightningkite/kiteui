package com.lightningkite.kiteui


actual object PlatformStorage {
    actual fun get(key: String): String? = null
    actual fun set(key: String, value: String): Unit {}
    actual fun remove(key: String): Unit {}
}
