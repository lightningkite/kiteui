package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction

public expect class FormattedTextInput(context: RContext) : RViewWithAction {
    public var enabled: Boolean
    public val content: ImmediateWritable<String>
    public var hint: String
    public var align: Align
    public var keyboardHints: KeyboardHints

    public fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    )
}