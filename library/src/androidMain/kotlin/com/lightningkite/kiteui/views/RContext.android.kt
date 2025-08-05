package com.lightningkite.kiteui.views

import android.content.res.Configuration
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public actual class RContext(public val activity: KiteUiActivity): RContextHelper() {
    public actual override val darkMode: Boolean?
        get() = when(activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> null
        }
    public actual fun split(): RContext = RContext(activity).also { it.addons.putAll(addons) }
    public actual var immersiveMode: Boolean = false
}