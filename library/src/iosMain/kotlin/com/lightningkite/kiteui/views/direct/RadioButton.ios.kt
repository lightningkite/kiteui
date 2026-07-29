package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIControl
import platform.UIKit.accessibilityTraits
import platform.UIKit.accessibilityValue
import platform.UIKit.isAccessibilityElement
import platform.UIKit.setAccessibilityTraits
import platform.UIKit.setAccessibilityValue
import platform.UIKit.setIsAccessibilityElement


public actual class RadioButton actual constructor(context: ElementContext) : NativeContainerElementWithAction(context) {
    actual override val underlyingNativeElement: RadioButton get() = this

    override val driverValue: String? get() = radioDriverValue()
    override val driverActions get() = super.driverActions + radioDriverActions()
    override val native: WrapperView = WrapperView()    // Todo: Unneeded wrapper?
    public val button = FrameLayoutButton()
    override val addChildTarget get() = button
    override val control: UIControl get() = button

    init {
        button.isAccessibilityElement = true
        button.accessibilityTraits = button.accessibilityTraits or UIAccessibilityTraitButton
        button.extensionHorizontalAlign = Align.Center
        button.extensionVerticalAlign = Align.Center
        native.addSubview(button)
    }

    private val _checked = Signal(false)
    public actual val checked: MutableReactiveValue<Boolean> get() = _checked

    init {
        _checked.addListener {
            button.accessibilityValue = if (_checked.value) "1" else "0"
        }
        @OptIn(ExperimentalKiteUi::class)
        themePipeline.add(
            ThemePipeline.Step.elementStyling,
            ThemeDerivation {
                it.copy(
                    id = "radbtn",
                    outline = it.icon,
                    iconOverride = it.foreground,
                    outlineWidth = maxOf(it.outlineWidth, 1.dp),
                    gap = it.gap / 4,
                    padding = it.padding / 4,
                    cornerRadii = CornerRadii.RatioOfSize(0.5f),
                ).withBack
            }
        )

        centered.icon {
            source = Icon.dot.resize(1.rem)
            ::description { if (this@RadioButton.checked()) "selected" else "unselected" }
            ::visible bind this@RadioButton.checked
        }

        onRemove(button.setOnClick {
            // Stay checked if already checked; don't allow radio buttons to become unchecked by clicking
            _checked.value = !_checked.value || _checked.value
        })
    }
}