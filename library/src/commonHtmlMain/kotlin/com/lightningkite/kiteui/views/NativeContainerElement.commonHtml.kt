@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi

public actual abstract class NativeContainerElement actual constructor(context: ElementContext) : NativeContainerElementCommonCode(context) {
    actual override fun nativeAddChild(index: Int, element: Element) {
        native.appendChild(index, element.underlyingNativeElement.native)
    }

    actual override fun nativeRemoveChild(index: Int) {
        native.removeChild(index)
    }

    actual override fun nativeClearChildren() {
        native.clearChildren()
    }
}