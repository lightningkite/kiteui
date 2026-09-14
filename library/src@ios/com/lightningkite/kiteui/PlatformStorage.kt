package com.lightningkite.kiteui

import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue

public actual object PlatformStorage {
    public actual fun get(key: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(key)
    public actual fun set(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, key)
    }
    public actual fun remove(key: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    }
}
