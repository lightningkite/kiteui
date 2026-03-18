package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*

actual class ProgrammaticLayout actual constructor(context: ElementContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.position = "relative"
    }
    actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) { field = value; invalidateLayout() }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        view.onRemove(view.native.resizeObserver().addListener {
            invalidateLayout()
        })
    }

    override fun internalRemoveChild(index: Int) {
        super.internalRemoveChild(index)
        invalidateLayout()
    }

    override fun internalClearChildren() {
        super.internalClearChildren()
        invalidateLayout()
    }


    actual fun invalidateLayout() {

    }
}