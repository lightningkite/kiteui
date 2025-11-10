package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color

enum class Platform {
    iOS, Android, Web, Desktop
    ;
    companion object
}
expect val Platform.Companion.current: Platform
expect val Platform.Companion.probablyAppleUser: Boolean
expect val Platform.Companion.usesTouchscreen: Boolean
expect val Platform.Companion.userAgent: String
expect val Platform.Companion.isSafari: Boolean
expect val Platform.Companion.isIOSSafari: Boolean

// Some projects require setting the status bar color dynamically. This method exists because KiteUI doesn't have an app
// level theme where it would make sense to dynamically set system theming parameters.
expect fun setStatusBarColor(color: Color)
