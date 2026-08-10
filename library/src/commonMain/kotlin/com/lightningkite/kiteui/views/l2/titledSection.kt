package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.contextAddon

public var ElementContext.titleDepth: Int by contextAddon(0)

@Deprecated("Use directly through context", ReplaceWith("context.titleDepth"))
public var ElementWriter.titleDepth: Int
    get() = context.titleDepth
    set(value) { context.titleDepth = value }

public fun ElementWriter.titledSection(
    title: String,
    content: RowOrCol.() -> Unit,
): Unit = titledSection({ this.content = title }, content)

public inline fun ElementWriter.titledSection(
    crossinline titleSetup: TextView.() -> Unit = {},
    content: RowOrCol.() -> Unit,
) {
    col {
        space(4.0)
        try {
            when (++context.titleDepth) {
                1 -> h1(titleSetup)
                2 -> h2(titleSetup)
                3 -> h3(titleSetup)
                4 -> h4(titleSetup)
                5 -> h5(titleSetup)
                else -> h6(titleSetup)
            }
            content()
        } finally {
            context.titleDepth--
        }
    }
}
