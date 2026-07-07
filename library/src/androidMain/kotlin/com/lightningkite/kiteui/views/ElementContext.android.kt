package com.lightningkite.kiteui.views

import android.content.res.Configuration
import com.lightningkite.kiteui.KiteUiActivity
import java.lang.ref.WeakReference

actual class ElementContext(activity: KiteUiActivity, parent: ElementContext? = null): ElementContextCommonCode(parent) {
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

    actual val darkMode: Boolean?
        get() = activityOrNull?.let {
            when(it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_YES -> true
                else -> null
            }
        }

    // by Claude - use addons.child() for lazy parent lookup instead of copying
    actual fun split() = activityOrNull?.let { ElementContext(it, parent = this) }
        ?: throw IllegalStateException("Cannot split RContext: Activity has been destroyed")

    actual var immersiveMode: Boolean = false
    actual companion object {}
}