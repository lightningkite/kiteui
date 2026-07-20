package com.lightningkite.kiteui

import android.content.pm.ApplicationInfo
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.views.AndroidAppContext

public actual val Platform.Companion.current: Platform
    get() = Platform.Android
public actual val Platform.Companion.probablyAppleUser: Boolean
    get() = false
public actual val Platform.Companion.usesTouchscreen: Boolean
    get() = true
public actual val Platform.Companion.userAgent: String
    get() = "Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"

// by Claude - check if the app was built with debuggable flag
public actual val Platform.Companion.isDevelopment: Boolean
    get() = (AndroidAppContext.applicationCtx?.applicationInfo?.flags ?: 0) and
            ApplicationInfo.FLAG_DEBUGGABLE != 0

public actual fun setStatusBarColor(color: Color) {
    val window = AndroidAppContext.activityCtx?.window

    // Check if we're on Android 11 (API 30) or higher for WindowInsetsController
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = window?.let { WindowCompat.getInsetsController(it, window.decorView) }

        if (controller != null) {
            if (isColorDark(color.toInt())) {
                controller.isAppearanceLightStatusBars = false
            } else {
                controller.isAppearanceLightStatusBars = true
            }
        }
    } else {
        // For older versions, fallback to systemUiVisibility method
        @Suppress("DEPRECATION")
        var flags = window?.decorView?.systemUiVisibility
        if (flags != null) {
            if (isColorDark(color.toInt())) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            window?.decorView?.systemUiVisibility = flags
        }
    }

    // Set the status bar color
    window?.statusBarColor = color.toInt()
}

public fun isColorDark(color: Int) : Boolean {
    val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(color)) / 255
    return darkness >= 0.5
}