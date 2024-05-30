package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ThemeChoice
import com.lightningkite.kiteui.views.ViewDsl
import platform.Foundation.NSURL
import platform.UIKit.UIApplication


actual class Link actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton()
    init {
        native.calculationContext = this
        native.onClick = {
            to().let {
                if(resetsStack) {
                    navigator.reset(it)
                } else {
                    navigator.navigate(it)
                }
            }
            onNavigate()
        }
    }

    actual var to: ()->Screen = { Screen.Empty }
    actual var navigator: ScreenStack = ScreenStack.main
    actual var newTab: Boolean = false
    actual var resetsStack: Boolean = false

    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }

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

