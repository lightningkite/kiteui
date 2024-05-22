package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.div
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon

actual class Checkbox actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutToggleButton()
    init {
        useBackground = UseBackground.Yes
        themeChoice = ThemeChoice.Derive {
            it.copy(
                outline = it.foreground,
                outlineWidth = maxOf(it.outlineWidth, 1.dp),
                spacing = it.spacing / 4,
                selected = { this },
                unselected = { this },
            )
        }
        icon(Icon.done, "") {
            ::visible.invoke { checked.await() }
        }
    }

    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) { native.enabled = value }
    actual val checked: Writable<Boolean> get() = native.checkedWritable

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
    }
    override fun getStateThemeChoice() = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        native.highlighted -> ThemeChoice.Derive { it.down() }
        native.focused -> ThemeChoice.Derive { it.hover() }
        else -> null
    }
}