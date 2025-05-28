package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.direct.RowOrCol

open class EdgeTouchHelper {
    open var top: Boolean = true
    open var bottom: Boolean = true
    open var left: Boolean = true
    open var right: Boolean = true
}

class LinearEdgeTouchHelper(
    val rowOrCol: RowOrCol
): EdgeTouchHelper() {
    fun onChildrenUpdated() {
        refresh_top()
        refresh_bottom()
        refresh_left()
        refresh_right()
    }
    private fun refresh_top() {
        val actualPadding = rowOrCol.paddingByEdge ?: rowOrCol.theme.padding
        val first = rowOrCol.children.firstOrNull() ?: return
        for(child in rowOrCol.children) {
            child.edgeTouchHelper.top = if(rowOrCol.vertical) {
                rowOrCol.edgeTouchHelper.top && actualPadding.top == 0.px && child === first
            } else {
                rowOrCol.edgeTouchHelper.top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
            }
        }
    }
    private fun refresh_bottom() {
        val actualPadding = rowOrCol.paddingByEdge ?: rowOrCol.theme.padding
        val last = rowOrCol.children.firstOrNull() ?: return
        for(child in rowOrCol.children) {
            if(rowOrCol.vertical) {
                child.edgeTouchHelper.bottom = rowOrCol.edgeTouchHelper.bottom && actualPadding.bottom == 0.px && child === last
            } else {
                child.edgeTouchHelper.bottom = rowOrCol.edgeTouchHelper.bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
            }
        }
    }
    private fun refresh_left() {
        val actualPadding = rowOrCol.paddingByEdge ?: rowOrCol.theme.padding
        val first = rowOrCol.children.firstOrNull() ?: return
        for(child in rowOrCol.children) {
            if(rowOrCol.vertical) {
                child.edgeTouchHelper.left = rowOrCol.edgeTouchHelper.left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
            } else {
                child.edgeTouchHelper.left = rowOrCol.edgeTouchHelper.left && actualPadding.left == 0.px && child === first
            }
        }
    }
    private fun refresh_right() {
        val actualPadding = rowOrCol.paddingByEdge ?: rowOrCol.theme.padding
        val last = rowOrCol.children.firstOrNull() ?: return
        for(child in rowOrCol.children) {
            if(rowOrCol.vertical) {
                child.edgeTouchHelper.right = rowOrCol.edgeTouchHelper.right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
            } else {
                child.edgeTouchHelper.right = rowOrCol.edgeTouchHelper.right && actualPadding.right == 0.px && child === last
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
    val parent: RView
): EdgeTouchHelper() {
    fun updateChild(child: RView) {
        val actualPadding = parent.paddingByEdge ?: parent.theme.padding
        child.edgeTouchHelper.top = parent.edgeTouchHelper.top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
        child.edgeTouchHelper.bottom = parent.edgeTouchHelper.bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
        child.edgeTouchHelper.left = parent.edgeTouchHelper.left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
        child.edgeTouchHelper.right = parent.edgeTouchHelper.right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
    }
    private fun refresh_top() {
        val actualPadding = parent.paddingByEdge ?: parent.theme.padding
        for(child in parent.children) {
            child.edgeTouchHelper.top = parent.edgeTouchHelper.top && actualPadding.top == 0.px && child.lastSetVerticalAlign.touchesStart
        }
    }
    private fun refresh_bottom() {
        val actualPadding = parent.paddingByEdge ?: parent.theme.padding
        for(child in parent.children) {
            child.edgeTouchHelper.bottom = parent.edgeTouchHelper.bottom && actualPadding.bottom == 0.px && child.lastSetVerticalAlign.touchesEnd
        }
    }
    private fun refresh_left() {
        val actualPadding = parent.paddingByEdge ?: parent.theme.padding
        for(child in parent.children) {
            child.edgeTouchHelper.left = parent.edgeTouchHelper.left && actualPadding.left == 0.px && child.lastSetHorizontalAlign.touchesStart
        }
    }
    private fun refresh_right() {
        val actualPadding = parent.paddingByEdge ?: parent.theme.padding
        for(child in parent.children) {
            child.edgeTouchHelper.right = parent.edgeTouchHelper.right && actualPadding.right == 0.px && child.lastSetHorizontalAlign.touchesEnd
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