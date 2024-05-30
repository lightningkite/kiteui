package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch

@OptIn(ExperimentalForeignApi::class)
actual class Switch actual constructor(context: RContext): RView(context) {
    override val native = UISwitch()

    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    actual val checked: Writable<Boolean>
        get() {
            return object : Writable<Boolean> {
                override val state: ReadableState<Boolean> get() = ReadableState(native.on)
                override fun addListener(listener: () -> Unit): () -> Unit {
                    return native.onEvent(this@Switch, UIControlEventValueChanged) { listener() }
                }

                override suspend fun set(value: Boolean) {
                    native.on = value
                }
            }
        }
}