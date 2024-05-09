package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class LocalDateField(context: RContext) : RView {
    val content: Writable<LocalDate?>
    var action: Action?
    var range: ClosedRange<LocalDate>?
}