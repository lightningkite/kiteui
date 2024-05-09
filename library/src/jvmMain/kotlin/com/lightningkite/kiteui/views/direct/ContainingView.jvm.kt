package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter

actual var ContainingView.vertical: Boolean
    get() = TODO("Not yet implemented")
    set(value) {}

@ViewDsl
actual fun ViewWriter.rowCollapsingToColumnActual(
    breakpoint: Dimension,
    setup: ContainingView.() -> Unit
) = col(setup)