package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class AutoCompleteTextField(context: RContext) : RViewWithAction {
    val content: ImmediateWritable<String>
    var keyboardHints: KeyboardHints
    var suggestions: List<String>
}