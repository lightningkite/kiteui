package com.lightningkite.kiteui.views

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ScrollView
import androidx.core.view.ScrollingView
import androidx.core.view.children
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NContext = Context
actual val NView.nContext: NContext get() = context
actual fun NView.removeNView(child: NView) {
    (this as ViewGroup).removeView(child)
    child.shutdown()
}

actual fun NView.listNViews(): List<NView> = (this as? ViewGroup)?.children?.toList() ?: listOf()

var animationsEnabled: Boolean = true
actual inline fun NView.withoutAnimation(action: () -> Unit) {
    if(!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        action()
    } finally {
        animationsEnabled = true
    }
}

actual fun NView.scrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
    afterTimeout(16) {
        generateSequence(this as NView) {
            it.parent as? NView
        }.firstOrNull {
            when (it) {
                is ScrollView -> {
                    scrollToView(it, this, animate)
                    true
                }

                is ScrollingView -> {
                    // TODO
                    true
                }

                else -> false
            }
        }
    }
}

/**
 * Used to scroll to the given view.
 *
 * @param scrollViewParent Parent ScrollView
 * @param view View to which we need to scroll.
 */
private fun scrollToView(scrollViewParent: ScrollView, view: View, smooth: Boolean) {
    // Get deepChild Offset
    val childOffset = Point()
    getDeepChildOffset(scrollViewParent, view.parent, view, childOffset)
    // Scroll to child.
    if(smooth) {
        scrollViewParent.smoothScrollTo(0, childOffset.y)
    } else {
        scrollViewParent.scrollTo(0, childOffset.y)
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
private fun getDeepChildOffset(mainParent: ViewGroup, parent: ViewParent, child: View, accumulatedOffset: Point) {
    val parentGroup = parent as ViewGroup
    accumulatedOffset.x += child.left
    accumulatedOffset.y += child.top
    if (parentGroup == mainParent) {
        return
    }
    getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
}

actual fun NView.consumeInputEvents() {
}

actual fun NView.nativeRequestFocus() {
    afterTimeout(16) {
        requestFocus()
    }
}