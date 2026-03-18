package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.*

typealias TextField = TextInput
expect class TextInput(context: ElementContext) : RViewWithAction {

    var enabled: Boolean
    val content: MutableReactiveValue<String>
    var keyboardHints: KeyboardHints
    var hint: String
    var align: Align?
}