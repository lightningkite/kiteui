package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.views.setSystemBarBackground
import com.lightningkite.kiteui.views.toUiColor

actual val Platform.Companion.probablyAppleUser: Boolean
    get() = true
actual val Platform.Companion.usesTouchscreen: Boolean
    get() = true

actual fun setStatusBarColor(color: Color) {
    setSystemBarBackground(color.toUiColor())
}