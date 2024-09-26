package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*

actual class ToggleButton actual constructor(context: RContext) : RView(context) {
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

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (checkedProp.value) t = t[SelectedSemantic]
        else t = t[UnselectedSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}
