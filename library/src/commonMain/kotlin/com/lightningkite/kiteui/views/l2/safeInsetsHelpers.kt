package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.identityHashCode
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.RView
import com.lightningkite.readable.reactive

fun RView.applySafeInsets(left: Boolean = true, top: Boolean = true, right: Boolean = true, bottom: Boolean = true) {
    println("$this applySafeInsets ${context.safeInsets.identityHashCode()} $left $top $right $bottom")
    reactive {
        val full = context.safeInsets()
        println("Safe insets activate")
        additionalPadding = Edges(
            left = if (left) full.left else 0.px,
            right = if (right) full.right else 0.px,
            top = if (top) full.top else 0.px,
            bottom = if (bottom) full.bottom else 0.px,
        )
    }
}