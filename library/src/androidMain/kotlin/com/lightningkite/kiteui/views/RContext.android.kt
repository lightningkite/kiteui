package com.lightningkite.kiteui.views

import android.content.res.Configuration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.OnControllableInsetsChangedListener
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.reactive.PlatformEventSharedReadable
import com.lightningkite.kiteui.reactive.Readable

actual class RContext(val activity: KiteUiActivity): RContextHelper() {
    actual override val darkMode: Boolean?
        get() = when(activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> null
        }
    private val insetsController = WindowInsetsControllerCompat(activity.window, activity.root.native)
    private var insetsListener: OnControllableInsetsChangedListener? = null
        set(value) {
            if (value == null) {
                insetsListener?.let(insetsController::removeOnControllableInsetsChangedListener)
            } else {
                insetsController.addOnControllableInsetsChangedListener(value)
            }
            field = value
        }
    override val keyboardVisible: Readable<Boolean> = PlatformEventSharedReadable(startup = { update ->
        insetsListener = OnControllableInsetsChangedListener { _, _ ->
            val keyboardVisible = ViewCompat.getRootWindowInsets(activity.root.native)?.isVisible(WindowInsetsCompat.Type.ime())
            keyboardVisible?.let { update(it) }
        }
        false
    }, shutdown = {
        insetsListener = null
    })

    actual fun split() = RContext(activity).also { it.addons.putAll(addons) }
}