package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@InternalKiteUi
public actual class ToggleButton public actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener { checkedProp.value = !checkedProp.value }
    }
    private val checkedProp = Signal(false)
    public actual val checked: MutableReactiveValue<Boolean> get() = checkedProp

    init {
        checked.addListener { refreshTheming() }
    }

    public actual var enabled: Boolean
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

    override fun applyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}
