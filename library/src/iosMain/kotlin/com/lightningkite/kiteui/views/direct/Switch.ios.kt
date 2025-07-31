package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.ReadableState
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
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
    public actual val checked: ImmediateWritable<Boolean>
        get() {
            return object : ImmediateWritable<Boolean> {
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