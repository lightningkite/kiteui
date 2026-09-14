package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver

@OptIn(ExperimentalKiteUi::class)
public actual class ToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = toggleDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + toggleDriverActions()
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener { checkedProp.value = !checkedProp.value }
    }
    private val checkedProp = Signal(false)
    public actual val checked: MutableReactiveValue<Boolean> get() = checkedProp

    init {
        checked.addListener {
            refreshTheming()
            ViewCompat.setStateDescription(native, if (checkedProp.value) "Pressed" else "Not pressed")
        }

        themePipeline.add(ThemePipeline.Step.elementStatus) {
            if (checkedProp.value) SelectedSemantic
            else UnselectedSemantic
        }
    }

    override fun nativeApplyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}
