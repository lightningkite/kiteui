package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.UIButton
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.await
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class MenuButton actual constructor(context: RContext): RView(context) {
    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override val native = FrameLayoutButton(this)

    actual fun opensMenu(action: ViewWriter.() -> Unit) {
        native.onClick = {
            val originalNavigator = navigator
            navigator.dialog.navigate(object : Screen {
                override fun ViewWriter.render() {
                    val dialogNavigator = navigator
                    dismissBackground {
                        centered - card - stack {
                            popoverClosers.add {
                                dialogNavigator.dismiss()
                            }
                            navigator = originalNavigator
                            action()
                            navigator = dialogNavigator
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
