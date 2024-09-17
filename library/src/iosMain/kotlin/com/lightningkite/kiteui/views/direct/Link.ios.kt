package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.FocusSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.views.*


actual class Link actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton(this)
    init {
        native.onClick = {
            to?.invoke()?.let {
                if(resetsStack) {
                    onNavigator.reset(it)
                } else {
                    onNavigator.navigate(it)
                }
                onNavigate()
            }
        }
    }

    actual var to: (() -> Screen)? = null
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

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return t
    }
}

