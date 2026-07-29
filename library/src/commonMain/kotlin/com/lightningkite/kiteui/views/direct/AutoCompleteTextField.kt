package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*


public expect class AutoCompleteTextField(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<String>
    public var keyboardHints: KeyboardHints
    public var suggestions: List<String>
}