package com.lightningkite.kiteui

import java.util.prefs.Preferences

/** Per-app storage in the OS's user preferences store.  [KiteUiRoot] sets the app id it's keyed by. */
public actual object PlatformStorage {
    internal var appId: String? = null
        set(value) {
            check(field == null || field == value) { "PlatformStorage already uses app id '$field'; a process runs one app, so it can't switch to '$value'." }
            field = value
        }

    internal val preferences: Preferences
        get() = Preferences.userRoot().node("kiteui/${checkNotNull(appId) { "PlatformStorage used before a KiteUiRoot set the app id." }}")

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
