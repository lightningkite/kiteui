package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction

actual class PhoneNumberInput actual constructor(context: RContext) : RViewWithAction(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val content: ImmediateWritable<String>
        get() = TODO("Not yet implemented")
    actual var hint: String
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var align: Align
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var textSize: Dimension
        get() = TODO("Not yet implemented")
        set(value) {}
}