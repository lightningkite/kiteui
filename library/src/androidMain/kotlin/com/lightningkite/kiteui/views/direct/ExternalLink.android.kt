package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.openTab
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch

actual class ExternalLink actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual var to: String? = null
        set(value) {
            field = value
            native.setOnClickListener { view ->
                launch {
                    onNavigate.invoke()
                    value?.let {
                        context.externalServices.openTab(it)
                    }
                }
            }
        }
    actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}