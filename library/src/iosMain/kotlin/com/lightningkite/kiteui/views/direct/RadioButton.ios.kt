package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class RadioButton actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayoutButton = FrameLayoutButton()
    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    private val _checked = Property(false)
    actual val checked: Writable<Boolean> get() = _checked

    init {
        useBackground = UseBackground.Yes
        themeChoice = ThemeChoice.Derive {
            it.copy(
                outline = it.foreground,
                outlineWidth = maxOf(it.outlineWidth, 1.dp),
                spacing = it.spacing / 4,
                cornerRadii = CornerRadii.RatioOfSize(0.5f),
                selected = { this },
                unselected = { this },
            )
        }
        icon(Icon.done, "") {
            ::visible.invoke { checked.await() }
        }
    }

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
        _checked.addListener { refreshTheming() }
        native.onClick = {
            _checked.value = true
        }
    }

    override fun getStateThemeChoice(): ThemeChoice? {
        return if (_checked.value) {
            when {
                !enabled -> ThemeChoice.Derive { it.selected().disabled() }
                native.highlighted -> ThemeChoice.Derive { it.selected().down() }
                native.focused -> ThemeChoice.Derive { it.selected().hover() }
                else -> ThemeChoice.Derive { it.selected() }
            }
        } else {
            when {
                !enabled -> ThemeChoice.Derive { it.unselected().disabled() }
                native.highlighted -> ThemeChoice.Derive { it.unselected().down() }
                native.focused -> ThemeChoice.Derive { it.unselected().hover() }
                else -> ThemeChoice.Derive { it.unselected() }
            }
        }
    }
}