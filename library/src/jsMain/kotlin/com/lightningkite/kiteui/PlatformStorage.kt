package com.lightningkite.kiteui

import kotlinx.browser.window

@InternalKiteUi
public actual object PlatformStorage {
    public actual fun get(key: String): String? {
        return window.localStorage.getItem(key)
    }

    public actual fun set(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    public actual fun remove(key: String) {
        window.localStorage.removeItem(key)
    }
}
