package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class LocalDateTimeField(context: RContext) : RViewWithAction {

    val content: ImmediateWritable<LocalDateTime?>
    var range: ClosedRange<LocalDateTime>?
}