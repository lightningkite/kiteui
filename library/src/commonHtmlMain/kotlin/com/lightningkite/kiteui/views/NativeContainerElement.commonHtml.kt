@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Dimension

actual abstract class NativeContainerElement actual constructor(context: ElementContext) : NativeContainerElementCommonCode(context) {
    actual override fun nativeAddChild(index: Int, element: Element) {
        // Apply parent's default alignment if child doesn't have explicit alignment set
        if (childDefaultAlignment != null) TODO("Set child default alignment")

        native.appendChild(index, element.underlyingNativeElement.native)
    }

    actual override fun nativeRemoveChild(index: Int) {
        native.removeChild(index)
    }

    actual override fun nativeClearChildren() {
        native.clearChildren()
    }
}