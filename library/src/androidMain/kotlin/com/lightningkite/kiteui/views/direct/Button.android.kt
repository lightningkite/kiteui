package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*


public actual class Button public actual constructor(context: RContext): RViewWithAction(context) {
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

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyThemeWithRipple(theme)
        val theme = theme.theme
        progress.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
    }

    init {
        native.setOnClickListener {
            if (enabled) {
                action?.startAction(this)
            }
        }
    }

    public actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}