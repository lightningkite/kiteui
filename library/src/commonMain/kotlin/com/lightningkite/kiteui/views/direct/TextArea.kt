package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class TextArea(context: RContext) : RView {

    var enabled: Boolean
    val content: ImmediateWritable<String>
    var keyboardHints: KeyboardHints
    var hint: String
}