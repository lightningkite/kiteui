package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class AutoCompleteTextField(context: RContext) : RView {
    val content: ImmediateWritable<String>
    var keyboardHints: KeyboardHints
    var action: Action?
    var suggestions: List<String>
}