package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement
import com.lightningkite.reactive.context.onRemove
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIControl

actual class ExternalLink actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
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

    actual var to: String? = null
    actual var newTab: Boolean = false

    init {
        onRemove(native.observe("highlighted") { refreshTheming() })
        onRemove(native.observe("selected") { refreshTheming() })
        onRemove(native.observe("enabled") { refreshTheming() })
    }
}
