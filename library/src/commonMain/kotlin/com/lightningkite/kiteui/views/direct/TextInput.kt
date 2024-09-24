package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView

typealias TextField = TextInput
expect class TextInput(context: RContext) : RView {

    var enabled: Boolean
    val content: ImmediateWritable<String>
    var keyboardHints: KeyboardHints
    var action: Action?
    var hint: String
    var align: Align
}