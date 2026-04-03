package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
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

    actual var to: String? = null

    actual var newTab: Boolean = false

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}