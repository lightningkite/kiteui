package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*


actual class Button actual constructor(context: RContext): RViewWithAction(context) {
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
        addChild(object: RView(context) {
            override val native = progress
        })
        working.addListener { progress.visibility = if(working.value) View.VISIBLE else View.GONE }
    }

    override fun applyForeground(theme: Theme) {
        progress.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
    }

    init {
        native.setOnClickListener {
            if (enabled) {
                action?.startAction(this)
            }
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}