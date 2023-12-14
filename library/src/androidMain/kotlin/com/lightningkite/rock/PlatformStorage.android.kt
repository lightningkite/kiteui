package com.lightningkite.rock

import android.content.Context
import android.content.SharedPreferences
import com.lightningkite.rock.views.AndroidAppContext

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object PlatformStorage {

    private val preferences: SharedPreferences by lazy {
        AndroidAppContext.applicationCtx.getSharedPreferences("Rock.PlatformStorage", Context.MODE_PRIVATE)
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