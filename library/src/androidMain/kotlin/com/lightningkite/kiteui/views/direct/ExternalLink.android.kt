package com.lightningkite.kiteui.views.direct

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*
import java.util.*

actual class ExternalLink actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual var to: String = ""
        set(value) {
            field = value
            native.setOnClickListener { view ->
                launch {
                    onNavigate.invoke()
                    ExternalServices.openTab(value)
                }
            }
        }
    actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun getStateThemeChoice(): ThemeChoice? = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        else -> null
    }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}