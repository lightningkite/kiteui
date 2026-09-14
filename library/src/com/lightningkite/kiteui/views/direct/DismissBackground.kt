package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement


public expect class DismissBackground(context: ElementContext) : NativeContainerElement {
    public fun onClick(action: suspend () -> Unit)
}