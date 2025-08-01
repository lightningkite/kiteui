package com.lightningkite.kiteui.views

import android.content.res.Configuration
import com.lightningkite.kiteui.KiteUiActivity

public actual class RContext(public val activity: KiteUiActivity): RContextHelper() {
    public actual override val darkMode: Boolean?
        get() = when(activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> null
        }
    public actual fun split(): RContext = RContext(activity).also { it.addons.putAll(addons) }
}