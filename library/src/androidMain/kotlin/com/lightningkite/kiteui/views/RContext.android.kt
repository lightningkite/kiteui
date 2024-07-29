package com.lightningkite.kiteui.views

import android.content.res.Configuration
import com.lightningkite.kiteui.KiteUiActivity

actual class RContext(val activity: KiteUiActivity): RContextHelper() {
    actual override val darkMode: Boolean?
        get() = when(activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> null
        }
    actual fun split() = RContext(activity).also { it.addons.putAll(addons) }
}