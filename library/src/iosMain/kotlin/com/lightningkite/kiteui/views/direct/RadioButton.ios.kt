package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon


actual class RadioButton actual constructor(context: RContext) : RView(context) {
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
    private val _checked = Property(false)
    actual val checked: ImmediateWritable<Boolean> get() = _checked

    init {
        themeChoice = ThemeDerivation {
            it.copy(
                id = "rad",
                outline = it.icon,
                iconOverride = it.foreground,
                outlineWidth = maxOf(it.outlineWidth, 1.dp),
                gap = it.gap / 4,
                padding = it.padding / 4,
                cornerRadii = CornerRadii.RatioOfSize(0.5f),
            ).withBack
        }
        centered - icon(Icon.dot.copy(width = 1.rem, height = 1.rem), "") {
            ::visible.invoke { checked() }
        }
        onRemove(button.setOnClick {
            _checked.value = !_checked.value
        })
    }
}