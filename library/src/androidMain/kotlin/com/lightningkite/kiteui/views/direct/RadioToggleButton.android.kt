package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Property
import com.lightningkite.kiteui.views.*

actual class RadioToggleButton actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener { checkedProp.value = true }
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

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (checkedProp.value) t = t[SelectedSemantic]
        else t = t[UnselectedSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) = super.applyThemeWithRipple(theme)
}
