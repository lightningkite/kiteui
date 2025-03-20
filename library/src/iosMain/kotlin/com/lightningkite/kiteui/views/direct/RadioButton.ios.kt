package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import kotlinx.cinterop.ExperimentalForeignApi


actual class RadioButton actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayoutButton = FrameLayoutButton()
    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
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
                spacing = it.spacing / 4,
                padding = it.padding / 4,
                cornerRadii = CornerRadii.RatioOfSize(0.5f),
            ).withBack
        }
        icon(Icon.dot, "") {
            ::visible.invoke { checked() }
        }
        onRemove(native.setOnClick {
            _checked.value = !_checked.value
        })
    }
}