package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class TextArea(context: RContext) : RViewWithAction {
    public var enabled: Boolean
    public val content: ImmediateWritable<String>
    public var keyboardHints: KeyboardHints
    public var hint: String
}