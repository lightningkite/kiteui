package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.runBlocking
import timber.log.Timber


actual class Link actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual var to: ()->Screen = { Screen.Empty }
        set(value) {
            field = value
            native.setOnClickListener { view ->
                if(resetsStack) {
                    navigator.reset(value())
                } else {
                    navigator.navigate(value())
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

    actual var navigator: ScreenStack = ScreenStack.main
    actual var resetsStack: Boolean = false

    override fun getStateThemeChoice(): ThemeChoice? = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        else -> null
    }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}


