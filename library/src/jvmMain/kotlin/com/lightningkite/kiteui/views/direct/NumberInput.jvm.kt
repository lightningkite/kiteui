package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue

actual class NumberInput actual constructor(context: RContext) :
    RViewWithAction(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val content: MutableReactiveValue<Double?>
        get() = TODO("Not yet implemented")
    actual var keyboardHints: KeyboardHints
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var hint: String
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var range: ClosedRange<Double>?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var align: Align
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}