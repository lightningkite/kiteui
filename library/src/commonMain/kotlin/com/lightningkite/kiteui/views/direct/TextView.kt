package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.TextOverflow
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class TextView(context: RContext) : RView {

    public var content: String
    public var align: Align
    public var ellipsis: Boolean
    public var wraps: Boolean
    public var wordBreak: WordBreak
    public fun setBasicHtmlContent(html: String)
}