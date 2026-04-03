package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.toggle
import platform.UIKit.UIControl


actual class ToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = toggleDriverValue()
    override val driverActions get() = super.driverActions + toggleDriverActions()
    override val native: FrameLayoutButton = FrameLayoutButton()
    override val control: UIControl get() = native

    private val _checked = Signal(false)
    actual val checked: MutableReactiveValue<Boolean> get() = _checked

    init {
        _checked.addListener { refreshTheming() }
        onRemove(native.setOnClick {
            _checked.toggle()
        })

        @OptIn(ExperimentalKiteUi::class)
        elementSpecificTheming += ElementSpecificTheming {
            if (_checked.value) SelectedSemantic
            else UnselectedSemantic
        }
    }
}
