package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch


actual class Switch actual constructor(context: ElementContext) : RView(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions get() = super.driverActions + switchDriverActions()
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
}