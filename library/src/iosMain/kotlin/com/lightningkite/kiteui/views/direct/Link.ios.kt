package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import com.lightningkite.reactive.context.onRemove
import kotlinx.coroutines.launch
import platform.UIKit.UIControl

actual class Link actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
    override val driverActions get() = super.driverActions + linkDriverActions()
    override val native = FrameLayoutButton()
    override val control: UIControl get() = native

    init {
        onRemove(native.setOnClick {
            launch {
                action?.startAction(this@launch)
                to?.invoke()?.let { to ->
                    onNavigateAction?.startAction(this@launch)
                    if (resetsStack) onNavigator.reset(to)
                    else onNavigator.navigate(to)
                }
            }
        })
    }

    actual var to: (() -> Page)? = null
    actual var onNavigator: PageNavigator = context.mainPageNavigator
    actual var newTab: Boolean = false
    actual var resetsStack: Boolean = false
}

