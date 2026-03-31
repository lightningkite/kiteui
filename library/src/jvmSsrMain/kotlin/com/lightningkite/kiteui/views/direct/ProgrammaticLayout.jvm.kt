package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*

actual class ProgrammaticLayout actual constructor(context: ElementContext) : NativeLinearLayoutElement(context) {
    init {
        native.tag = "div"
        native.style.position = "relative"
    }
    actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) { field = value; invalidateLayout() }
    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        element.onRemove(element.native.resizeObserver().addListener {
            invalidateLayout()
        })
    }

    override fun nativeRemoveChild(index: Int) {
        super.nativeRemoveChild(index)
        invalidateLayout()
    }

    override fun nativeClearChildren() {
        super.nativeClearChildren()
        invalidateLayout()
    }


    actual fun invalidateLayout() {

    }
}