package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.identityHashCode
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.safeInsets
import com.lightningkite.readable.ReactiveContext
import com.lightningkite.readable.reactive

fun RView.applySafeInsets(left: Boolean = true, top: Boolean = true, right: Boolean = true, bottom: Boolean = true) {
    val s = safeInsets
    reactive {
        val full = s()
        additionalPadding = Edges(
            left = if (left) full.left else 0.px,
            right = if (right) full.right else 0.px,
            top = if (top) full.top else 0.px,
            bottom = if (bottom) full.bottom else 0.px,
        )
    }
}
fun RView.applySafeInsets(mapper: ReactiveContext.(Edges)->Edges) {
    val s = safeInsets
    reactive {
        val full = s()
        additionalPadding = mapper(full)
    }
}