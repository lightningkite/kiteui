package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
actual class ToggleButton actual constructor(context: RContext) : RView(context) {
    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override val native: FrameLayoutButton = FrameLayoutButton(this)
    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    private val _checked = Property(false)
    actual val checked: Writable<Boolean> get() = _checked

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
        _checked.addListener { refreshTheming() }
        native.onClick = {
            _checked.value = !_checked.value
        }
    }
    override fun getStateThemeChoice(): ThemeChoice? {
        return if(_checked.value) {
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
