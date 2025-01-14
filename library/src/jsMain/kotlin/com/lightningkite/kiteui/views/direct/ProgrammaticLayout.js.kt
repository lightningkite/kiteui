package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.lens
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.*
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.position = "relative"
    }
    actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) { field = value; invalidateLayout() }

    init {
        onRemove(native.resizeObserver().addListener {
            println("Resize observer hit!")
            invalidateLayout()
        })
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        view.native.onElement { it.asDynamic().__existingMeasure = null }
        view.native.style.position = "absolute"
        view.onRemove(view.native.mutationObserver(true).addListener {
            view.native.onElement { it.asDynamic().__existingMeasure = null }
            if(timeoutSet) return@addListener
            invalidateLayout()
        })
        invalidateLayout()
    }

    override fun internalRemoveChild(index: Int) {
        super.internalRemoveChild(index)
        invalidateLayout()
    }

    override fun internalClearChildren() {
        super.internalClearChildren()
        invalidateLayout()
    }

    private val inProgress = object: ProgrammingLayoutInProgress {
        override fun measure(child: RView, sizeConstraint: Size): Size {
            val e = child.native.element as? HTMLElement ?: return Size(0.0, 0.0)
            val existing = e.asDynamic().__existingMeasure as? Size
            val existingConstraint = e.asDynamic().__existingMeasureConstraint as? Size
            if(existing != null && existingConstraint == sizeConstraint) return existing
            println("Measuring element...")
            val m = e.measure(sizeConstraint)
            e.asDynamic().__existingMeasure = m
            e.asDynamic().__existingMeasureConstraint = sizeConstraint
            return m
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
            if(
                child.asDynamic().__last_left == left &&
                child.asDynamic().__last_top == top &&
                child.asDynamic().__last_right == right &&
                child.asDynamic().__last_bottom == bottom) {
                // avoid adjusting style because it's expensive
                return
            }
            child.native.suppressMutationObserverForStyle {
                child.native.style.position = "absolute"
                child.native.style.left = left.toString() + "px"
                child.native.style.top = top.toString() + "px"
                child.native.style.width = (right - left).toString() + "px"
                child.native.style.height = (bottom - top).toString() + "px"
            }
            child.asDynamic().__last_left = left
            child.asDynamic().__last_top = top
            child.asDynamic().__last_right = right
            child.asDynamic().__last_bottom = bottom
        }

        override fun existingPosition(child: RView): Rect = Rect.fromSize(
            left = child.native.element?.scrollLeft ?: child.native.style.left?.removeSuffix("px")?.toDoubleOrNull() ?: 0.0,
            top = child.native.element?.scrollTop ?: child.native.style.top?.removeSuffix("px")?.toDoubleOrNull() ?: 0.0,
            width = child.native.element?.scrollWidth?.toDouble() ?: child.native.style.width?.removeSuffix("px")?.toDoubleOrNull() ?: 0.0,
            height = child.native.element?.scrollHeight?.toDouble() ?: child.native.style.height?.removeSuffix("px")?.toDoubleOrNull() ?: 0.0,
        )
    }

    private var timeoutSet = false
    actual fun invalidateLayout() {
        if(timeoutSet) return
        window.setTimeout({
            // TODO: this really isn't the perfect way to get constraints and might cause wrapping size problems
            val parentSize = native?.element?.let { Size(it.clientWidth.toDouble(), it.clientHeight.toDouble()) }
                ?: return@setTimeout
            val m = delegate.measure(this, inProgress, parentSize)
            native.style.width = "${m.width}px"
            native.style.height = "${m.height}px"
            delegate.layout(this, inProgress, m)
            window.setTimeout({
                timeoutSet = false
            }, 1)
        }, 1)
        timeoutSet = true
    }
}