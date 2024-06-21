package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class MenuButton actual constructor(context: RContext): RView(context) {
    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override val native = FrameLayoutButton(this)

    actual fun opensMenu(createMenu: ViewWriter.() -> Unit) {
        native.onClick = {
            dialogScreenNavigator.navigate(object : Screen {
                override fun ViewWriter.render() {
                    dismissBackground {
                        centered - card - stack {
                            popoverLayer(
                                closer = { dialogScreenNavigator.dismiss() },
                                createPopover = createMenu
                            )
                        }
                    }
                }
            })
        }
    }
    actual var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    actual var requireClick: Boolean = true
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
    }
    override fun getStateThemeChoice(): ThemeChoice? = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        native.highlighted -> ThemeChoice.Derive { it.down() }
        native.focused -> ThemeChoice.Derive { it.hover() }
        else -> null
    }
}
