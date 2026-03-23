package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.safeInsets
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive

fun Element.applySafeInsets(left: Boolean = true, top: Boolean = true, right: Boolean = true, bottom: Boolean = true) {
    val s = context.safeInsets
    reactive {
        val full = s()
        safeAreaPadding = Edges(
            left = if (left) full.left else 0.px,
            right = if (right) full.right else 0.px,
            top = if (top) full.top else 0.px,
            bottom = if (bottom) full.bottom else 0.px,
        )
    }
}
fun Element.applySafeInsets(mapper: ReactiveContext.(Edges)->Edges) {
    val s = context.safeInsets
    reactive {
        val full = s()
        safeAreaPadding = mapper(full)
    }
}