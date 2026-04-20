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
import platform.UIKit.isAccessibilityElement
import platform.UIKit.setAccessibilityTraits
import platform.UIKit.setAccessibilityValue
import platform.UIKit.setIsAccessibilityElement


actual class Checkbox actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    actual override val underlyingNativeElement: Checkbox get() = this

    override val driverValue: String? get() = checkboxDriverValue()
    override val driverActions get() = super.driverActions + checkboxDriverActions()
    override val native: WrapperView = WrapperView()
    val button = FrameLayoutButton()
    override val control: UIControl get() = button
    override val addChildTarget get() = button

    init {
        setupControl()
        button.isAccessibilityElement = true
        button.accessibilityTraits = button.accessibilityTraits or UIAccessibilityTraitButton
        button.extensionHorizontalAlign = Align.Center
        button.extensionVerticalAlign = Align.Center
        native.addSubview(button)
    }

    private val _checked = Signal(false)
    actual val checked: MutableReactiveValue<Boolean> get() = _checked

    init {
        _checked.addListener {
            button.accessibilityValue = if (_checked.value) "1" else "0"
        }
        @OptIn(ExperimentalKiteUi::class)
        themePipeline.add(
            ThemePipeline.Step.elementStyling,
            ThemeDerivation {
                it.copy(
                    id = "chkbox",
                    outline = it.icon,
                    iconOverride = it.foreground,
                    outlineWidth = maxOf(it.outlineWidth, 1.dp),
                    gap = it.gap / 4,
                    padding = it.padding / 4,
                ).withBack
            }
        )
        icon {
            source = Icon.done.resize(1.rem)
            ::description { if (this@Checkbox.checked()) "checked" else "unchecked" }
            ::visible bind this@Checkbox.checked
        }
        onRemove(button.setOnClick {
            _checked.toggle()
        })
    }
}
