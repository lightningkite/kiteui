package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.*


expect class TextArea(context: ElementContext) : RViewWithAction {
    var enabled: Boolean
    val content: MutableReactiveValue<String>
    var keyboardHints: KeyboardHints
    var hint: String
}