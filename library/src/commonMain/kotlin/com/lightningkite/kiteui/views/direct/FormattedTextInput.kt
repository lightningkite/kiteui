package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.TextFormat
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*

public expect class FormattedTextInput(context: ElementContext) : NativeElementWithAction {
    /**
     * The data characters of the shown text.
     *
     * Setting it shows the data as laid out by [format], so reading it back gives only the data that layout kept.
     */
    public val content: MutableReactiveValue<String>
    public var hint: String
    public var align: Align?
    public var keyboardHints: KeyboardHints

    /** The format applied as text is typed or set; [TextFormat.None] by default. Changing it reformats the current data. */
    public var format: TextFormat
}

@Deprecated(
    "Set the format property instead: format = TextFormat(isRawData, formatter)",
    ReplaceWith("format = TextFormat(isRawData, formatter)", "com.lightningkite.kiteui.models.TextFormat")
)
public fun FormattedTextInput.format(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String) {
    format = TextFormat(isRawData, formatter)
}
