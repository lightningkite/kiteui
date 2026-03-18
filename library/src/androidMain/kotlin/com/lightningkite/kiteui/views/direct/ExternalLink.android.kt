package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

actual class ExternalLink actual constructor(context: ElementContext) : RView(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
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
                        ExternalServices.openTab(it)
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