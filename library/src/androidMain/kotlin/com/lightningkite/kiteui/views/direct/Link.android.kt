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


public actual class Link public actual constructor(context: RContext): RView(context) {
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
    }

    public actual var to: (() -> Page)? = null
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
    public actual var newTab: Boolean = false
    private var onNavigate: (suspend () -> Unit)? = null
    public actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    private var onClick: (suspend () -> Unit)? = null
    public actual fun onClick(action: suspend () -> Unit): Unit {
        onClick = action
    }

    public actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    public actual var onNavigator: PageNavigator = mainPageNavigator
    public actual var resetsStack: Boolean = false

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}


