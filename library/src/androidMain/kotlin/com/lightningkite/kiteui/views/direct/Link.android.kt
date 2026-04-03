package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class Link actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
    override val driverActions get() = super.driverActions + linkDriverActions()
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    init {
        native.setOnClickListener { _ ->
            launch {
                action?.startAction(this@launch)
                to?.invoke()?.let { to ->
                    onNavigateAction?.startAction(this@launch)

                    if (resetsStack) onNavigator.reset(to)
                    else onNavigator.navigate(to)
                }
            }
        }
    }

    actual var to: (() -> Page)? = null
    actual var newTab: Boolean = false
    actual var onNavigator: PageNavigator = context.mainPageNavigator
    actual var resetsStack: Boolean = false

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}


