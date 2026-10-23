package com.lightningkite.kiteui.telemetry

import android.content.Context
import com.lightningkite.kiteui.views.AndroidAppContext

internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(throwable)
        } catch (_: Exception) {}
        previous?.uncaughtException(thread, throwable)
    }
}

// PlatformStorage uses apply(), which writes asynchronously and is lost if the process dies first.
// Same preferences file as PlatformStorage.
internal actual fun writeDurably(key: String, value: String) {
    AndroidAppContext.applicationCtx.getSharedPreferences("KiteUI.PlatformStorage", Context.MODE_PRIVATE)
        .edit().putString(key, value).commit()
}
