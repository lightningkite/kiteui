package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch
import com.lightningkite.kiteui.views.AiDriver

public actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + externalLinkDriverActions()
    override val native: FrameLayout = FrameLayout(context.activity).apply {
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

    // Validated on assignment for the same reason as on web: this is the sink. `openLink` resolves
    // the URL through an Intent, so an unchecked scheme like `intent:` or `file:` from untrusted
    // content would reach another installed app or local storage.
    public actual var to: String? = null
        set(value) {
            field = safeLinkUrlOrNull(value)
        }

    public actual var newTab: Boolean = false

    override fun nativeSetAction(action: Action?) {
        native.contentDescription = accessibleLabel ?: action?.title
    }

    override fun nativeApplyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}