package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.rContextAddon

var ViewWriter.titleDepth: Int by rContextAddon(0)

@ViewDsl
fun ViewWriter.titledSection(
    title: String,
    content: RowOrCol.() -> Unit,
) = titledSection({ this.content = title }, content)

@ViewDsl
fun ViewWriter.titledSection(
    titleSetup: TextView.() -> Unit = {},
    content: RowOrCol.() -> Unit,
) {
    col {
        space(4.0)
        try {
            when (++titleDepth) {
                1 -> h1 { titleSetup() }
                2 -> h2 { titleSetup() }
                3 -> h3 { titleSetup() }
                4 -> h4 { titleSetup() }
                5 -> h5 { titleSetup() }
                else -> h6 { titleSetup() }
            }
            content()
        } finally {
            titleDepth--
        }
    }
}
