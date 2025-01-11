package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

enum class ScrollReason {
    NotScrolling,
    ScrollBar,
    Drag,
    Programmatic
}

expect class ScrollView(context: RContext, horizontal: Boolean, vertical: Boolean) : RView {
    val horizontal: Boolean
    val vertical: Boolean
    var showScrollBars: Boolean
    val scrollReason: Readable<ScrollReason>
    val viewport: Readable<Rect>
    val content: Readable<Rect>
    fun scrollTo(left: Double, top: Double, animated: Boolean)

    /**
     * Should not interrupt animations.
     */
    fun offset(x: Double, y: Double)
}
