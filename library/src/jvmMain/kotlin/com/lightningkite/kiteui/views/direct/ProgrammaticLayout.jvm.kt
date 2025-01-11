package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.*

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
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