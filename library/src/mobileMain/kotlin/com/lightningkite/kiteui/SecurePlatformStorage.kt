package com.lightningkite.kiteui

/**
 * Like [PlatformStorage], but for secrets such as tokens: encrypted at rest and kept to this device.
 * The Keychain on iOS; on Android, values encrypted with an Android Keystore key in app-private preferences.
 *
 * Only on Android and iOS. A browser has nothing a page's script can read that other script on the page can't,
 * and server-side rendering serves many users from one process.
 */
public expect object SecurePlatformStorage {
    public fun get(key: String): String?
    /**
     * Stores [value] under [key].
     *
     * With [ensureOnDisk], the value is written before this returns, so it survives the process dying right after.
     */
    public fun set(key: String, value: String, ensureOnDisk: Boolean = false)
    public fun remove(key: String)
}
