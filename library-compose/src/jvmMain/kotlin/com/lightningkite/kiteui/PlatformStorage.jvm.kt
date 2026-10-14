package com.lightningkite.kiteui

import java.util.prefs.Preferences

/** Per-app storage in the OS's user preferences store.  [KiteUiRoot] sets the app id it's keyed by. */
public actual object PlatformStorage {
    internal var appId: String? = null

    private val preferences: Preferences by lazy {
        val id = checkNotNull(appId) { "PlatformStorage used before a KiteUiRoot set the app id." }
        Preferences.userRoot().node("kiteui/$id")
    }

    public actual fun get(key: String): String? = preferences.get(key, null)

    public actual fun set(key: String, value: String) {
        preferences.put(key, value)
        preferences.flush()
    }

    public actual fun remove(key: String) {
        preferences.remove(key)
        preferences.flush()
    }
}
