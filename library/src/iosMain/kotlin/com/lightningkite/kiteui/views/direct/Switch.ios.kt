package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.reactive.core.MutableReactiveValue
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch
import platform.UIKit.isAccessibilityElement
import platform.UIKit.setIsAccessibilityElement
import com.lightningkite.kiteui.views.AiDriver

public actual class Switch actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + switchDriverActions()
    override val native: UISwitch = UISwitch()
    override val control: UIControl get() = native
    init {
        // UISwitch already has built-in VoiceOver support (traits, value announcements).
        // Ensure isAccessibilityElement is set so our accessibleLabel propagates.
        native.isAccessibilityElement = true
        setupControl()
    }

    public actual val checked: MutableReactiveValue<Boolean> = object : MutableReactiveValue<Boolean> {
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(this@Switch, UIControlEventValueChanged, listener)
        }

        override var value: Boolean
            get() = native.on
            set(value) {
                if (native.on != value) {
                    native.on = value
                    // fire change event so reactive listeners are notified on programmatic updates
                    native.sendActionsForControlEvents(UIControlEventValueChanged)
                }
            }
    }
}