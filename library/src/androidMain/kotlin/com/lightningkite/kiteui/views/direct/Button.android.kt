package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*


actual class Button actual constructor(context: RContext): RView(context) {
    val progress = ProgressBar(context.activity, null, android.R.attr.progressBarStyleSmall).apply {
        minimumWidth = 0
        minimumHeight = 0
        visibility = View.GONE
    }
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    override fun postSetup() {
        super.postSetup()
        native.addView(progress)
        working.addListener { progress.visibility = if(working.value) View.VISIBLE else View.GONE }
    }

    override fun applyForeground(theme: Theme) {
        progress.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
    }

    actual fun onClick(action: suspend () -> Unit) {
        native.setOnClickListener { view ->
            if (enabled) {
                launchManualCancel {
                    enabled = false
                    try { action() } finally { enabled = true }
                }
            }
        }
    }

    actual var enabled: Boolean
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