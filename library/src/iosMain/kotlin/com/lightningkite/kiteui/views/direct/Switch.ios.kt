package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch


public actual class Switch public actual constructor(context: RContext) : RView(context) {
    override val native = UISwitch()

    public actual inline var enabled: Boolean
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