package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch


actual class Switch actual constructor(context: RContext) : RView(context) {
    override val native = UISwitch()

    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    actual val checked: MutableReactiveValue<Boolean>
        get() {
            return object : MutableReactiveValue<Boolean> {
                override fun addListener(listener: () -> Unit): () -> Unit {
                    return native.onEvent(this@Switch, UIControlEventValueChanged, listener)
                }

                override var value: Boolean
                    get() = native.on
                    set(value) {
                        if (native.on != value)
                            native.on = value
                    }
            }
        }

    // by Claude
    override var accessibilityValue: String?
        get() = readBooleanValue(checked)
        set(value) = writeBooleanValue(checked, value)
    override val accessibilityActions get() = CLICK_AND_SET_VALUE_ACTIONS
    override fun performAccessibilityAction(action: String, value: String?) =
        performBooleanAction(checked, action, value) { a, v -> super.performAccessibilityAction(a, v) }
}