package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithAction
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*


public expect class TextArea(context: ElementContext) : ElementWithAction, NativeElement {
    public val content: MutableReactiveValue<String>
    public var keyboardHints: KeyboardHints
    public var hint: String

    override var action: Action?
    override var enabled: Boolean
}