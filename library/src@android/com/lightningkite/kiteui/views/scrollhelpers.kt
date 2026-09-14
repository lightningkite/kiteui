package com.lightningkite.kiteui.views

import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.view.ScrollingView
import androidx.core.widget.NestedScrollView


/**
 * Used to scroll to the given view.
 *
 * @param scrollViewParent Parent ScrollView
 * @param view View to which we need to scroll.
 */
internal fun scrollToView(scrollViewParent: ScrollView, view: View, smooth: Boolean) {
    // Get deepChild Offset
    val childOffset = Point()
    getDeepChildOffset(scrollViewParent, view.parent, view, childOffset)
    // Scroll to child.
    if (smooth) {
        scrollViewParent.smoothScrollTo(0, childOffset.y.plus(view.height / 2).minus(scrollViewParent.height / 2))
    } else {
        scrollViewParent.scrollTo(0, childOffset.y.plus(view.height / 2).minus(scrollViewParent.height / 2))
    }
}

/**
 * Used to scroll to the given view.
 *
 * @param scrollViewParent Parent ScrollView
 * @param view View to which we need to scroll.
 */
internal fun scrollToView(scrollViewParent: NestedScrollView, view: View, smooth: Boolean) {
    // Get deepChild Offset
    val childOffset = Point()
    getDeepChildOffset(scrollViewParent, view.parent, view, childOffset)
    // Scroll to child.
    if (smooth) {
        scrollViewParent.smoothScrollTo(0, childOffset.y.plus(view.height / 2).minus(scrollViewParent.height / 2))
    } else {
        scrollViewParent.scrollTo(0, childOffset.y.plus(view.height / 2).minus(scrollViewParent.height / 2))
    }
}

/**
 * Used to scroll to the given view.
 *
 * @param scrollViewParent Parent ScrollView
 * @param view View to which we need to scroll.
 */
internal fun scrollToView(scrollViewParent: HorizontalScrollView, view: View, smooth: Boolean) {
    // Get deepChild Offset
    val childOffset = Point()
    getDeepChildOffset(scrollViewParent, view.parent, view, childOffset)
    // Scroll to child.
    if (smooth) {
        scrollViewParent.smoothScrollTo(childOffset.x.plus(view.width / 2).minus(scrollViewParent.width / 2), 0)
    } else {
        scrollViewParent.scrollTo(childOffset.x.plus(view.width / 2).minus(scrollViewParent.width / 2), 0)
    }
}

/**
 * Used to get deep child offset.
 *
 *
 * 1. We need to scroll to child in scrollview, but the child may not the direct child to scrollview.
 * 2. So to get correct child position to scroll, we need to iterate through all of its parent views till the main parent.
 *
 * @param mainParent        Main Top parent.
 * @param parent            Parent.
 * @param child             Child.
 * @param accumulatedOffset Accumulated Offset.
 */
internal fun getDeepChildOffset(mainParent: ViewGroup, parent: ViewParent, child: View, accumulatedOffset: Point) {
    val parentGroup = parent as ViewGroup
    accumulatedOffset.x += child.left
    accumulatedOffset.y += child.top
    if (parentGroup == mainParent) {
        return
    }
    getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
}