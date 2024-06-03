package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*

actual class ToggleButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener { checkedProp.value = !checkedProp.value }
    }
    private val checkedProp = Property(false)
    actual val checked: ImmediateWritable<Boolean> get() = checkedProp

    init {
        checked.addListener { refreshTheming() }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun getStateThemeChoice(): ThemeChoice? = (if(checkedProp.value) ThemeChoice.Derive { it.selected() } else ThemeChoice.Derive { it.unselected() }) +
            if(enabled) null else ThemeChoice.Derive { it.disabled() }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}
