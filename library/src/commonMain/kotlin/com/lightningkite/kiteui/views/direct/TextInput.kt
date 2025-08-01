package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction

public typealias TextField = TextInput
public expect class TextInput(context: RContext) : RViewWithAction {

    public var enabled: Boolean
    public val content: ImmediateWritable<String>
    public var keyboardHints: KeyboardHints
    public var hint: String
    public var align: Align
}