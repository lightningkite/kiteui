package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*


actual class Checkbox actual constructor(context: ElementContext) : RView(context) {
    override val driverValue: String? get() = checkboxDriverValue()
    override val driverActions get() = super.driverActions + checkboxDriverActions()
    override val native: WrapperView = WrapperView()
    val button = FrameLayoutButton()
    override val addChildTarget get() = button
    init {
        button.extensionHorizontalAlign = Align.Center
        button.extensionVerticalAlign = Align.Center
        native.addSubview(button)
    }

    actual inline var enabled: Boolean
        get() = button.enabled
        set(value) {
            button.enabled = value
        }
    private val _checked = Signal(false)
    actual val checked: MutableReactiveValue<Boolean> get() = _checked

    init {
        themeChoice = ThemeDerivation {
            it.copy(
                id = "rad",
                outline = it.icon,
                iconOverride = it.foreground,
                outlineWidth = maxOf(it.outlineWidth, 1.dp),
                gap = it.gap / 4,
                padding = it.padding / 4,
            ).withBack
        }
        icon(Icon.done.copy(width = 1.rem, height = 1.rem), "") {
            ::visible.invoke { checked() }
        }
        onRemove(button.setOnClick {
            _checked.value = !_checked.value
        })
    }
}
