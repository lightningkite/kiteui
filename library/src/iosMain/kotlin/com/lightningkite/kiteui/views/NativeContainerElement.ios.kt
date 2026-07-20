package com.lightningkite.kiteui.views

import platform.UIKit.UIView
import kotlin.math.max

public actual abstract class NativeContainerElement actual constructor(context: ElementContext) : ContainerElement, NativeContainerElementCommonCode(context) {
    actual override fun nativeAddChild(index: Int, element: Element) {
        val existingView = children.getOrNull(index)
        val existingIndex = addChildTarget.subviews.indexOfFirst { it == existingView?.native }
        if (existingIndex == -1)
            addChildTarget.addSubview(element.native)
        else
            addChildTarget.insertSubview(element.native, existingIndex.toLong())
    }

    actual override fun nativeRemoveChild(index: Int) {
        if (index >= children.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${addChildTarget.subviews.size}")
        }
        children[index].native.removeFromSuperview()
    }

    actual override fun nativeClearChildren() {
        children.toList().forEach {
            it.native.removeFromSuperview()
        }
    }

    protected open val mySpacing get() = theme.gap

    override fun refreshPadding() {
        super.refreshPadding()
        val gap = max(mySpacing.value, padding?.value ?: 0.0)
        for (child in children) {
            child.native.layoutLayers(gap)
        }
    }
}