package com.lightningkite.kiteui.views

import android.content.res.Configuration
import com.lightningkite.kiteui.KiteUiActivity
import java.lang.ref.WeakReference

public actual class ElementContext(activity: KiteUiActivity, parent: ElementContext? = null): ElementContextCommonCode(parent) {
    private val activityRef = WeakReference(activity)

    /**
     * Returns the Android activity. Throws [IllegalStateException] if it was destroyed.
     *
     * Use [activityOrNull] if it may be gone.
     */
    public val activity: KiteUiActivity
        get() = activityRef.get() ?: throw IllegalStateException("Activity has been destroyed")

    /**
     * Returns the Android activity, or `null` if it was destroyed.
     */
    public val activityOrNull: KiteUiActivity?
        get() = activityRef.get()

    public actual val darkMode: Boolean?
        get() = activityOrNull?.let {
            when(it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_YES -> true
                else -> null
            }
        }

    public actual val root: ElementContext = parent?.root ?: this

    // by Claude - use addons.child() for lazy parent lookup instead of copying
    public actual fun child(): ElementContext = activityOrNull?.let { ElementContext(it, parent = this) }
        ?: throw IllegalStateException("Cannot make a child ElementContext: Activity has been destroyed")

    public actual var immersiveMode: Boolean = false
    public actual companion object : kotlin.coroutines.CoroutineContext.Key<ElementContext>
}