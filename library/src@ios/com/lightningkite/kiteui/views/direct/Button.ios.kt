package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.context.reactive
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIControl
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setAccessibilityTraits
import com.lightningkite.kiteui.views.AiDriver

@OptIn(ExperimentalKiteUi::class)
public actual class Button actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + buttonDriverActions()
    override val native: FrameLayoutButton = FrameLayoutButton()
    override val control: UIControl get() = native

    init {
        activityIndicator {
            opacity = 0.0
            // `working` is a NativeElement extension, so an unqualified call here resolves against the
            // innermost receiver - the ActivityIndicator - whose foreground process set is always empty
            // (the action is watched on the button, and process state does not propagate between
            // elements). The spinner has to read the button's state explicitly or it never shows.
            ::opacity.invoke { if (this@Button.working()) 1.0 else 0.0 }
            native.extensionSizeConstraints = SizeConstraints(minWidth = null, minHeight = null)
        }
    }

    override fun nativeSetAction(action: Action?) {
        native.accessibilityLabel = accessibleLabel ?: action?.title
        onRemove(native.setOnClick {
            action?.startAction(this)
        })
    }

    override fun nativeSetSecondaryAction(action: Action?) {
        onRemove(native.setOnLongPress {
            action?.startAction(this)
        })
    }

    init {
        native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitButton
        setupControl()
        ::opacity { if (loading()) 0.7 else 1.0 }
    }
}