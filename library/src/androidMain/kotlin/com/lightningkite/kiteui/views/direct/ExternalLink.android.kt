package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

public actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    init {
        native.setOnClickListener { _ ->
            launch {
                action?.startAction(this@launch)
                to?.let {
                    onNavigateAction?.startAction(this@launch)
                    context.externalServices.openLink(it, newTab)
                }
            }
        }
    }

    public actual var to: String? = null

    public actual var newTab: Boolean = false

    override fun nativeSetAction(action: Action?) {
        native.contentDescription = accessibleLabel ?: action?.title
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}