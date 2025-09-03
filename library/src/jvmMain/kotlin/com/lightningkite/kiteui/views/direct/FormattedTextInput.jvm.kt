package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue

actual class FormattedTextInput actual constructor(context: RContext) :
    RViewWithAction(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val content: MutableReactiveValue<String>
        get() = TODO("Not yet implemented")
    actual var hint: String
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var align: Align
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var keyboardHints: KeyboardHints
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun format(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}