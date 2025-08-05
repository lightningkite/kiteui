package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@InternalKiteUi
public actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
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