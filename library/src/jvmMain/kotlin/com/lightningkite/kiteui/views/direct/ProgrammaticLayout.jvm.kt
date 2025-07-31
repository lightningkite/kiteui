package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.signal.Constant
import com.lightningkite.signal.Readable
import com.lightningkite.signal.onRemove
import com.lightningkite.kiteui.views.*

public actual class ProgrammaticLayout public actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    init {
        native.tag = "div"
        native.style.position = "relative"
    }
    public actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
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


    public actual fun invalidateLayout() {

    }
}