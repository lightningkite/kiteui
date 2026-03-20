package com.lightningkite.kiteui.views

actual abstract class NativeContainerElement actual constructor(context: ElementContext) : ContainerElement,
    NativeContainerElementCommonCode() {
    actual override fun nativeAddChild(index: Int, element: Element) {
    }

    actual override fun nativeRemoveChild(index: Int) {
    }

    actual override fun nativeClearChildren() {
    }
}