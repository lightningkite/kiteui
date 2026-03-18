package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.*


expect class AutoCompleteTextField(context: ElementContext) : RViewWithAction {
    val content: MutableReactiveValue<String>
    var keyboardHints: KeyboardHints
    var suggestions: List<String>
}