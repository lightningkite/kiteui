package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class Link actual constructor(context: ElementContext): RView(context) {
    override val driverActions get() = super.driverActions + linkDriverActions()
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual var to: (() -> Page)? = null
        set(value) {
            field = value
            native.setOnClickListener { view ->
                onClick?.let { launch { it() } }
                value?.invoke()?.let { it ->
                    launch {
                        onNavigate?.invoke()
                        if (resetsStack) {
                            onNavigator.reset(it)
                        } else {
                            onNavigator.navigate(it)
                        }
                    }
                }
            }
        }
    actual var newTab: Boolean = false
    private var onNavigate: (suspend () -> Unit)? = null
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    private var onClick: (suspend () -> Unit)? = null
    actual fun onClick(action: suspend () -> Unit): Unit {
        onClick = action
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    actual var onNavigator: PageNavigator = mainPageNavigator
    actual var resetsStack: Boolean = false

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}


