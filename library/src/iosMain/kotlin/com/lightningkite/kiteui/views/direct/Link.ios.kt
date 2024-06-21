package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.views.*


actual class Link actual constructor(context: RContext): RView(context) {
    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override val native = FrameLayoutButton(this)
    init {
        native.onClick = {
            to().let {
                if(resetsStack) {
                    onNavigator.reset(it)
                } else {
                    onNavigator.navigate(it)
                }
            }
            onNavigate()
        }
    }

    actual var to: ()->Screen = { Screen.Empty }
    actual var onNavigator: ScreenNavigator = mainScreenNavigator
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
    override fun getStateThemeChoice(): ThemeChoice? = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        native.highlighted -> ThemeChoice.Derive { it.down() }
        native.focused -> ThemeChoice.Derive { it.hover() }
        else -> null
    }
}

