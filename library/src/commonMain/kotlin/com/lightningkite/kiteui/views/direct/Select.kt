package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.Readable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class Select(context: RContext) : RView {
    var enabled: Boolean
    fun <T> bind(edits: Writable<T>, data: Readable<List<T>>, render: (T) -> String)
}