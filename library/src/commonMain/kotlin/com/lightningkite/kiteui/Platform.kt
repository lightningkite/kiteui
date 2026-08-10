package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

public enum class Platform {
    iOS, Android, Web, Desktop
    ;
    public companion object
}
public expect val Platform.Companion.current: Platform
public expect val Platform.Companion.probablyAppleUser: Boolean
public expect val Platform.Companion.usesTouchscreen: Boolean
public expect val Platform.Companion.userAgent: String

// by Claude - true when running in a development/debug context (debug builds, localhost, etc.)
public expect val Platform.Companion.isDevelopment: Boolean

// Some projects require setting the status bar color dynamically. This method exists because KiteUI doesn't have an app
// level theme where it would make sense to dynamically set system theming parameters.
public expect fun setStatusBarColor(color: Color)
