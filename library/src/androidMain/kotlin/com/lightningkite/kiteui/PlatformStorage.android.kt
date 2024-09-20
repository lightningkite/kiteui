package com.lightningkite.kiteui

import android.content.Context
import android.content.SharedPreferences
import com.lightningkite.kiteui.views.AndroidAppContext


actual object PlatformStorage {

    private val preferences: SharedPreferences by lazy {
        AndroidAppContext.applicationCtx.getSharedPreferences("KiteUI.PlatformStorage", Context.MODE_PRIVATE)
    }

    actual fun get(key: String): String? {
        return preferences.getString(key, null)
    }

    actual fun set(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}