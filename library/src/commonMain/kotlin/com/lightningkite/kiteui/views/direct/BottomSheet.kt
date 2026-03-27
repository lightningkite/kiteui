package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementWriter

expect fun ElementWriter.openBottomSheet(
    halfScreenRatio: Float = 0.5f,
    dim: Boolean = true,
    view: ElementWriter.CanAddTheme.() -> Unit
)