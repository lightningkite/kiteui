package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.direct.ProgrammaticLayout
import com.lightningkite.kiteui.views.direct.RowCollapsingToColumn
import com.lightningkite.kiteui.views.direct.RowOrCol
import kotlinx.coroutines.NonCancellable.children

open class EdgeTouchHelper(val view: RView) {
    open var top: Boolean = true
    open var bottom: Boolean = true
    open var left: Boolean = true
    open var right: Boolean = true

    var willApplyPadding = false
    open fun onChildrenUpdated(){
        willApplyPadding = (left || top || right || bottom) && view.children.any { it.cannotBeCovered }
    }
    open fun onChildUpdated(child: RView) = onChildrenUpdated()

    open val paddingToApply: Edges? get() = if(willApplyPadding) {
        Edges(
            left = if(left) (view.safeInsets?.left ?: 0.px) else 0.px,
            top = if(top) (view.safeInsets?.top ?: 0.px) else 0.px,
            right = if(right) (view.safeInsets?.right ?: 0.px) else 0.px,
            bottom = if(bottom) (view.safeInsets?.bottom ?: 0.px) else 0.px,
        )
    } else null
}

class LinearEdgeTouchHelper(
    val rowOrCol: RowOrCol
): EdgeTouchHelper(rowOrCol) {
    override fun onChildrenUpdated() {
        super.onChildrenUpdated()
        refresh_top()
        refresh_bottom()
        refresh_left()
        refresh_right()
        rowOrCol.refreshPadding()
        for(child in rowOrCol.children) child.refreshPadding()
    }
    private fun refresh_top() {
        val actualPadding = rowOrCol.appliedPadding
        val first = rowOrCol.children.firstOrNull { it.shown } ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.top = !willApplyPadding && if(rowOrCol.vertical) {
                top && actualPadding.top == 0.px && child === first
            } else {
                top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
            }
        }
    }
    private fun refresh_bottom() {
        val actualPadding = rowOrCol.appliedPadding
        val last = rowOrCol.children.lastOrNull { it.shown } ?: return
        val anyWeighted = rowOrCol.children.any { it.lastSetWeight != null }
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.bottom = !willApplyPadding &&  if(rowOrCol.vertical) {
                bottom && actualPadding.bottom == 0.px && child === last && anyWeighted
            } else {
                bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
            }
        }
    }
    private fun refresh_left() {
        val actualPadding = rowOrCol.appliedPadding
        val first = rowOrCol.children.firstOrNull { it.shown } ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.left = !willApplyPadding &&  if(rowOrCol.vertical) {
                left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
            } else {
                left && actualPadding.left == 0.px && child === first
            }
        }
    }
    private fun refresh_right() {
        val actualPadding = rowOrCol.appliedPadding
        val last = rowOrCol.children.lastOrNull { it.shown }  ?: return
        val anyWeighted = rowOrCol.children.any { it.lastSetWeight != null }
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.right = !willApplyPadding &&  if(rowOrCol.vertical) {
                right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
            } else {
                right && actualPadding.right == 0.px && child === last && anyWeighted
            }
        }
    }
    override var top: Boolean
        get() = super.top
        set(value) {
            if(value != super.top) {
                super.top = value
                refresh_top()
            }
        }
    override var bottom: Boolean
        get() = super.bottom
        set(value) {
            if(value != super.bottom) {
                super.bottom = value
                refresh_bottom()
            }
        }
    override var left: Boolean
        get() = super.left
        set(value) {
            if(value != super.left) {
                super.left = value
                refresh_left()
            }
        }
    override var right: Boolean
        get() = super.right
        set(value) {
            if(value != super.right) {
                super.right = value
                refresh_right()
            }
        }
}
class RowCollapsingEdgeTouchHelper(
    val rowOrCol: RowCollapsingToColumn
): EdgeTouchHelper(rowOrCol) {
    override fun onChildrenUpdated() {
        super.onChildrenUpdated()
        refresh_top()
        refresh_bottom()
        refresh_left()
        refresh_right()
        rowOrCol.refreshPadding()
        for(child in rowOrCol.children) child.refreshPadding()
    }
    private fun refresh_top() {
        val actualPadding = rowOrCol.appliedPadding
        val first = rowOrCol.children.firstOrNull { it.shown } ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.top = !willApplyPadding && if(true /*TODO: rowOrCol.vertical*/) {
                top && actualPadding.top == 0.px && child === first
            } else {
                top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
            }
        }
    }
    private fun refresh_bottom() {
        val actualPadding = rowOrCol.appliedPadding
        val last = rowOrCol.children.lastOrNull { it.shown } ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.bottom = !willApplyPadding && if(true /*TODO: rowOrCol.vertical*/) {
                bottom && actualPadding.bottom == 0.px && child === last
            } else {
                bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
            }
        }
    }
    private fun refresh_left() {
        val actualPadding = rowOrCol.appliedPadding
        val first = rowOrCol.children.firstOrNull { it.shown } ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.left = !willApplyPadding && if(true /*TODO: rowOrCol.vertical*/) {
                left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
            } else {
                left && actualPadding.left == 0.px && child === first
            }
        }
    }
    private fun refresh_right() {
        val actualPadding = rowOrCol.appliedPadding
        val last = rowOrCol.children.lastOrNull { it.shown } ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.right = !willApplyPadding && if(true /*TODO: rowOrCol.vertical*/) {
                right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
            } else {
                right && actualPadding.right == 0.px && child === last
            }
        }
    }
    override var top: Boolean
        get() = super.top
        set(value) {
            if(value != super.top) {
                super.top = value
                refresh_top()
            }
        }
    override var bottom: Boolean
        get() = super.bottom
        set(value) {
            if(value != super.bottom) {
                super.bottom = value
                refresh_bottom()
            }
        }
    override var left: Boolean
        get() = super.left
        set(value) {
            if(value != super.left) {
                super.left = value
                refresh_left()
            }
        }
    override var right: Boolean
        get() = super.right
        set(value) {
            if(value != super.right) {
                super.right = value
                refresh_right()
            }
        }
}

class FrameEdgeTouchHelper(
    val parent: RView,
    val log: Console? = null,
): EdgeTouchHelper(parent) {
    override fun onChildrenUpdated() {
        log?.log("$parent onChildrenUpdated - ${parent.children.joinToString { it.toString()}}")
        super.onChildrenUpdated()
        refresh_top()
        refresh_bottom()
        refresh_left()
        refresh_right()
        parent.refreshPadding()
        for(child in parent.children) child.refreshPadding()
    }
    override fun onChildUpdated(child: RView) {
        log?.log("$parent onChildUpdated $child")
        super.onChildrenUpdated()
        val actualPadding = parent.appliedPadding
        child.edgeTouchHelper.top = top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
        child.edgeTouchHelper.bottom = bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
        child.edgeTouchHelper.left = left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
        child.edgeTouchHelper.right = right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
    }
    private fun refresh_top() {
        val actualPadding = parent.appliedPadding
        for(child in parent.children) {
            if(child == viewDebugTarget)
                println("child.edgeTouchHelper.top = !willApplyPadding($willApplyPadding) && top($top) && actualPadding.top(${actualPadding.top}) == 0.px && child.lastSetVerticalAlign.touchesStart(${child.lastSetVerticalAlign.touchesStart})")
            child.edgeTouchHelper.top = !willApplyPadding && top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
        }
    }
    private fun refresh_bottom() {
        val actualPadding = parent.appliedPadding
        for(child in parent.children) {
            if(child == viewDebugTarget)
                println("child.edgeTouchHelper.bottom = !willApplyPadding($willApplyPadding) && bottom($bottom) && actualPadding.bottom(${actualPadding.bottom}) == 0.px && child.lastSetVerticalAlign.touchesEnd(${child.lastSetVerticalAlign.touchesEnd})")
            child.edgeTouchHelper.bottom = !willApplyPadding && bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
        }
    }
    private fun refresh_left() {
        val actualPadding = parent.appliedPadding
        for(child in parent.children) {
            if(child == viewDebugTarget)
                println("child.edgeTouchHelper.left = !willApplyPadding($willApplyPadding) && left($left) && actualPadding.left(${actualPadding.left}) == 0.px && child.lastSetHorizontalAlign.touchesStart(${child.lastSetHorizontalAlign.touchesStart})")
            child.edgeTouchHelper.left = !willApplyPadding && left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
        }
    }
    private fun refresh_right() {
        val actualPadding = parent.appliedPadding
        for(child in parent.children) {
            if(child == viewDebugTarget)
                println("child.edgeTouchHelper.right = !willApplyPadding($willApplyPadding) && right($right) && actualPadding.right(${actualPadding.right}) == 0.px && child.lastSetHorizontalAlign.touchesEnd(${child.lastSetHorizontalAlign.touchesEnd})")
            child.edgeTouchHelper.right = !willApplyPadding && right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
        }
    }
    override var top: Boolean
        get() = super.top
        set(value) {
            if(value != super.top) {
                super.top = value
                refresh_top()
            }
        }
    override var bottom: Boolean
        get() = super.bottom
        set(value) {
            if(value != super.bottom) {
                super.bottom = value
                refresh_bottom()
            }
        }
    override var left: Boolean
        get() = super.left
        set(value) {
            if(value != super.left) {
                super.left = value
                refresh_left()
            }
        }
    override var right: Boolean
        get() = super.right
        set(value) {
            if(value != super.right) {
                super.right = value
                refresh_right()
            }
        }
}