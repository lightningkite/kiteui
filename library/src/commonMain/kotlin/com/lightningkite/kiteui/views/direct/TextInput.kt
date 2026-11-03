package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*

@Deprecated("Renamed", ReplaceWith("TextInput")) public typealias TextField = TextInput

public expect class TextInput(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<String>
    public var keyboardHints: KeyboardHints
    public var hint: String
    public var align: Align?
}