package com.lightningkite.kiteui.views

import android.view.ViewGroup
import androidx.core.view.ViewCompat

public actual abstract class NativeContainerElement actual constructor(context: ElementContext) : NativeContainerElementCommonCode(context) {
    abstract override val native: ViewGroup

    actual override fun nativeAddChild(index: Int, element: Element) {
        native.addView(element.native, index)
        if (fullyStarted) ViewCompat.requestApplyInsets(element.native)
        if (native.childCount != children.size) throw IllegalStateException("nativeAddChild($index $element) failed on $this: Native child count ${native.childCount} != Element count ${children.size} on ${this::class.qualifiedName}")
    }

    actual override fun nativeRemoveChild(index: Int) {
        if (native.childCount != children.size) throw IllegalStateException("nativeRemoveChild($index) failed on $this: Native child count ${native.childCount} != Element count ${children.size} on ${this::class.qualifiedName}")
        native.removeViewAt(index)
    }

    actual override fun nativeClearChildren() {
        if (native.childCount != children.size) throw IllegalStateException("nativeClearChildren() failed on $this: Native child count ${native.childCount} != Element count ${children.size} on ${this::class.qualifiedName}")
        native.removeAllViews()
    }
}