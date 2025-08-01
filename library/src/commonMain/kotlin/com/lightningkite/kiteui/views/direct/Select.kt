package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.contracts.*
import kotlin.jvm.JvmInline


expect class Select(context: RContext) : RView {
    var enabled: Boolean
    fun <T> bind(edits: MutableReactive<T>, data: Reactive<List<T>>, render: (T) -> String)
}