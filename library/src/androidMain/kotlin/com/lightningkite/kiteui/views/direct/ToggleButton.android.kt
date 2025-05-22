package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Property
import com.lightningkite.kiteui.views.*

actual class ToggleButton actual constructor(context: RContext) : RView(context), InputAccessibility {
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

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (checkedProp.value) t = t[SelectedSemantic]
        else t = t[UnselectedSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)

    override var ariaRequired: Boolean? = null
        set(value) {
            field = value
            // Android doesn't have a direct equivalent to aria-required
            // We could update the contentDescription to include "required" if needed
            if (value == true && native.contentDescription != null) {
                val currentDesc = native.contentDescription.toString()
                if (!currentDesc.contains("(required)")) {
                    native.contentDescription = "$currentDesc (required)"
                }
            }
        }
}
