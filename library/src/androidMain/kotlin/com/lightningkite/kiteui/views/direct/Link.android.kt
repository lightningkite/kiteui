package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.views.*


actual class Link actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual var to: ()->Screen = { Screen.Empty }
        set(value) {
            field = value
            native.setOnClickListener { view ->
                if(resetsStack) {
                    onNavigator.reset(value())
                } else {
                    onNavigator.navigate(value())
                }
                launchManualCancel { onNavigate() }
            }
        }
    actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    actual var onNavigator: ScreenNavigator = mainScreenNavigator
    actual var resetsStack: Boolean = false

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}


