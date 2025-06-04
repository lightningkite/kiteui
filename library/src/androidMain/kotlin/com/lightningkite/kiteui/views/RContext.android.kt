package com.lightningkite.kiteui.views

import android.content.res.Configuration
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.readable.Property
import com.lightningkite.readable.Readable

actual class RContext(val activity: KiteUiActivity): RContextHelper() {
    actual override val darkMode: Boolean?
        get() = when(activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> null
        }
    actual fun split() = RContext(activity).also { it.addons.putAll(addons) }
    actual var immersiveMode: Boolean = false
}