package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

public actual class ExternalLink public actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
    }

    public actual var to: String? = null
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
    public actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    public actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    public actual var enabled: Boolean
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

    override fun applyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}