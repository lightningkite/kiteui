package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.openTab
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElement(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    init {
        elementSpecificTheming += ElementSpecificTheming {
            var t: ThemeDerivation = ClickableSemantic
            if (!enabled) t += DisabledSemantic
            t
        }
    }

    actual var to: String? = null
        set(value) {
            field = value
            native.setOnClickListener { view ->
                launch {
                    onNavigate.invoke()
                    value?.let {
                        context.openTab(it)
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

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}