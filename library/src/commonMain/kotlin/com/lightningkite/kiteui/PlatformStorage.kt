package com.lightningkite.kiteui

public expect object PlatformStorage {
    public fun get(key: String): String?
    /**
     * Stores [value] under [key].
     *
     * With [ensureOnDisk], the value is written before this returns, so it survives the process dying right after.
     */
    public fun set(key: String, value: String, ensureOnDisk: Boolean = false)
    public fun remove(key: String)
}
