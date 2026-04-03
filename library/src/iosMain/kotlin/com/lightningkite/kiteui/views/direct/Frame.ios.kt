package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement

actual class Frame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    override val native = FrameLayout()
}