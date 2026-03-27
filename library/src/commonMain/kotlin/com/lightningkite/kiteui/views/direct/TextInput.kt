package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*

@Deprecated("Renamed", ReplaceWith("TextInput")) typealias TextField = TextInput

expect class TextInput(context: ElementContext) : NativeElementWithAction {
    val content: MutableReactiveValue<String>
    var keyboardHints: KeyboardHints
    var hint: String
    var align: Align?
}