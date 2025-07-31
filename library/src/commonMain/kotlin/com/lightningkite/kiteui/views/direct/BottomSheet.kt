package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

public expect fun ViewWriter.openBottomSheet(
    halfScreenRatio: Float = 0.5f,
    dim: Boolean = true,
    view: ViewWriter.() -> ViewModifiable
)