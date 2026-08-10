package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import com.lightningkite.reactive.context.onRemove
import kotlinx.coroutines.launch
import platform.UIKit.UIAccessibilityTraitLink
import platform.UIKit.UIControl
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityTraits
import com.lightningkite.kiteui.views.AiDriver

public actual class Link actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + linkDriverActions()
    override val native: FrameLayoutButton = FrameLayoutButton()
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

    init {
        native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitLink
        setupControl()
    }

    public actual var to: (() -> Page)? = null
    public actual var onNavigator: PageNavigator = context.mainPageNavigator
    public actual var newTab: Boolean = false
    public actual var resetsStack: Boolean = false
}

