package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.toggle
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIControl
import platform.UIKit.accessibilityTraits
import platform.UIKit.accessibilityValue
import platform.UIKit.setAccessibilityTraits
import platform.UIKit.setAccessibilityValue


public actual class ToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = toggleDriverValue()
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + toggleDriverActions()
    override val native: FrameLayoutButton = FrameLayoutButton()
    override val control: UIControl get() = native

    private val _checked = Signal(false)
    public actual val checked: MutableReactiveValue<Boolean> get() = _checked

    init {
        native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitButton
        setupControl()
        _checked.addListener {
            refreshTheming()
            native.accessibilityValue = if (_checked.value) "1" else "0"
        }
        onRemove(native.setOnClick {
            _checked.toggle()
        })

        @OptIn(ExperimentalKiteUi::class)
        themePipeline.add(ThemePipeline.Step.elementStatus) {
            if (_checked.value) SelectedSemantic
            else UnselectedSemantic
        }
    }
}
