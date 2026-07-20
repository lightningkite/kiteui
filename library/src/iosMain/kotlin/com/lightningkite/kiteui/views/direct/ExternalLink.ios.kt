package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement
import com.lightningkite.reactive.context.onRemove
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIAccessibilityTraitLink
import platform.UIKit.UIApplication
import platform.UIKit.UIControl
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityTraits

public actual class ExternalLink actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
    override val native = FrameLayoutButton()
    override val control: UIControl get() = native

    init {
        onRemove(native.setOnClick {
            launch {
                action?.startAction(this@launch)
                to?.let { to ->
                    onNavigateAction?.startAction(this@launch)
                    UIApplication.sharedApplication.openURL(NSURL(string = to), mapOf<Any?, Any?>()) {}
                }
            }
        })
    }

    public actual var to: String? = null
    public actual var newTab: Boolean = false

    init {
        native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitLink
        setupControl()
    }
}
