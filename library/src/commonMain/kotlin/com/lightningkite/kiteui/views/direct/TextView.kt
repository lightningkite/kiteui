package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.TextOverflow
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect abstract class TextView(context: RContext) : RView {

    var content: String
    var align: Align
    var textSize: Dimension
    var ellipsis: Boolean
    var wraps: Boolean
}
expect class HeaderView(context: RContext, level: Int) : TextView {
}
expect class BodyTextView(context: RContext) : TextView {
}
expect class SubTextView(context: RContext) : TextView {
}