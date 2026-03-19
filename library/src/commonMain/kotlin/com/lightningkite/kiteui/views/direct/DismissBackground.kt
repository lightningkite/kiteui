package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement


expect class DismissBackground(context: ElementContext) : NativeContainerElement {
    fun onClick(action: suspend () -> Unit)
}