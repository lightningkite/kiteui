package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class RadioToggleButton actual constructor(context: ElementContext) : NativeContainerElement(context) {
    override val driverValue: String? get() = radioToggleDriverValue()
    override val driverActions get() = super.driverActions + radioToggleDriverActions()
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener { checkedProp.value = true }
    }
    private val checkedProp = Signal(false)
    actual val checked: MutableReactiveValue<Boolean> get() = checkedProp

    init {
        checked.addListener { refreshTheming() }

        elementSpecificTheming += ElementSpecificTheming {
            var t: ThemeDerivation = ClickableSemantic
            if (checkedProp.value) t += SelectedSemantic
            else t += UnselectedSemantic
            if (!enabled) t += DisabledSemantic
            t
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}
