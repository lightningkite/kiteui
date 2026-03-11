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
import java.lang.ref.WeakReference

actual class RContext(activity: KiteUiActivity): RContextHelper() {
    private val activityRef = WeakReference(activity)

    /**
     * Returns the activity if it's still alive, or throws IllegalStateException if destroyed.
     * Use this property when you absolutely need the activity and can't continue without it.
     */
    val activity: KiteUiActivity
        get() = activityRef.get() ?: throw IllegalStateException("Activity has been destroyed")

    /**
     * Returns the activity if it's still alive, or null if destroyed.
     * Use this property when you want to check if the activity is still available.
     */
    val activityOrNull: KiteUiActivity?
        get() = activityRef.get()

    actual override val darkMode: Boolean?
        get() = activityOrNull?.let {
            when(it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_YES -> true
                else -> null
            }
        }

    // by Claude - use addons.child() for lazy parent lookup instead of copying
    actual fun split() = activityOrNull?.let { RContext(it).also { it.addons = addons.child() } }
        ?: throw IllegalStateException("Cannot split RContext: Activity has been destroyed")

    actual var immersiveMode: Boolean = false
    actual companion object {}
}