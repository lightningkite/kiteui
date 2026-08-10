package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.AiDriver

@OptIn(ExperimentalKiteUi::class)
public actual class Button actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + buttonDriverActions()
    internal val progress: ProgressBar = ProgressBar(context.activity, null, android.R.attr.progressBarStyleSmall).apply {
        minimumWidth = 0
        minimumHeight = 0
        visibility = View.GONE
    }
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
    }

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
        addChild(object: NativeElement(context) {
            override val native = this@Button.progress
        })
        foregroundProcesses.addListener { progress.visibility = if(!foregroundProcesses.state.ready) View.VISIBLE else View.GONE }
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        applyThemeWithRipple(theme)
        val theme = theme.theme
        progress.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
    }

    override fun nativeSetAction(action: Action?) {
        native.contentDescription = accessibleLabel ?: action?.title
    }

    init {
        native.setOnClickListener {
            if (enabled) {
                action?.startAction(this)
            }
        }
        native.setOnLongClickListener {
            if (enabled) {
                secondaryAction?.startAction(this)
                secondaryAction != null
            } else {
                false
            }
        }
    }
}