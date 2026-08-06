package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement
import com.lightningkite.reactive.context.onRemove
import kotlinx.coroutines.launch
import com.lightningkite.kiteui.Log
import platform.Foundation.NSURL
import platform.UIKit.UIAccessibilityTraitLink
import platform.UIKit.UIApplication
import platform.UIKit.UIControl
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityTraits

public actual class ExternalLink actual constructor(context: ElementContext): NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + externalLinkDriverActions()
    override val native: FrameLayoutButton = FrameLayoutButton()
    override val control: UIControl get() = native

    init {
        onRemove(native.setOnClick {
            launch {
                action?.startAction(this@launch)
                to?.let { to ->
                    onNavigateAction?.startAction(this@launch)
                    UIApplication.sharedApplication.openURL(NSURL(string = to), mapOf<Any?, Any?>()) { opened ->
                        // A URL that passed validation but the OS declines to open is worth saying
                        // out loud. It is indistinguishable, on screen, from a link this library
                        // deliberately refused - and the usual cause is benign: the Simulator ships
                        // no Mail or Phone app, so `mailto:` and `tel:` never open there however
                        // correct the link is.
                        if (!opened) Log.warn("ExternalLink: iOS declined to open '$to' - no installed app handles that scheme")
                    }
                }
            }
        })
    }

    // Validated on assignment for the same reason as on web: this is the sink. `openURL` will
    // launch any installed app that registered the scheme, so an unchecked custom scheme from
    // untrusted content hands control to a third-party app of the attacker's choosing.
    public actual var to: String? = null
        set(value) {
            field = safeLinkUrlOrNull(value)
        }

    public actual var newTab: Boolean = false

    init {
        native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitLink
        setupControl()
    }
}
