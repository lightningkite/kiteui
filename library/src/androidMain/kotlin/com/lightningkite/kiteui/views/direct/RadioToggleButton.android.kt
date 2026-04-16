package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

@OptIn(ExperimentalKiteUi::class)
actual class RadioToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = radioToggleDriverValue()
    override val driverActions get() = super.driverActions + radioToggleDriverActions()
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener { checkedProp.value = true }
    }
    private val checkedProp = Signal(false)
    actual val checked: MutableReactiveValue<Boolean> get() = checkedProp

    init {
        checked.addListener {
            refreshTheming()
            ViewCompat.setStateDescription(native, if (checkedProp.value) "Selected" else "Not selected")
        }

        themePipeline.add(ThemePipeline.Step.elementStatus) {
            if (checkedProp.value) SelectedSemantic
            else UnselectedSemantic
        }
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}
