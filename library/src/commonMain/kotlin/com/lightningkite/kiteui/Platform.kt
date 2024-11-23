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

expect fun setStatusBarColor(color: Color)
