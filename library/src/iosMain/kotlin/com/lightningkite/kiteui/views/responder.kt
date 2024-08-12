package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.direct.ScrollLayout
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView

fun UIView.findFirstResponderChild(): UIView? {
    if(isFirstResponder) return this
    else return subviews.asSequence().mapNotNull { (it as UIView).findFirstResponderChild() }.firstOrNull()
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.scrollToMe(animated: Boolean = false) {
    generateSequence(superview) { it.superview }.filterIsInstance<ScrollLayout>().firstOrNull()?.let {
        // goal: centers equal
        val rect = it.convertRect(frame, fromView = superview)
        val visible = CGRectMake(
            x = it.contentOffset.useContents { x },
            y = it.contentOffset.useContents { y },
            width = it.bounds.useContents { size.width },
            height = it.bounds.useContents { size.height },
        )

        fun CValue<CGRect>.start() = if(it.horizontal) useContents { origin.x } else useContents { origin.y }
        fun CValue<CGRect>.end() = if(it.horizontal) useContents { origin.x + size.width } else useContents { origin.y + size.height }
        fun CValue<CGRect>.size() = if(it.horizontal) useContents { size.width } else useContents { size.height }
        if (rect.start() < visible.start()) {
            // Scrolling backwards
            it.setContentOffset(CGPointMake(
                if(it.horizontal) rect.start().coerceAtLeast(0.0) else 0.0,
                if(!it.horizontal) rect.start().coerceAtLeast(0.0) else 0.0,
            ), animated = animated)
        } else if(rect.end() > visible.end()) {
            // Scrolling forwards
            it.setContentOffset(CGPointMake(
                if(it.horizontal) (rect.end().coerceAtMost(it.contentSize.useContents { width }) - visible.size()) else 0.0,
                if(!it.horizontal) (rect.end().coerceAtMost(it.contentSize.useContents { height }) - visible.size()) else 0.0,
            ), animated = animated)
        } else {
            // Cool, that's great.  It's already in view.
        }

    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.scrollToMeCenter(animated: Boolean = false) {
    generateSequence(superview) { it.superview }.filterIsInstance<ScrollLayout>().firstOrNull()?.let {
        // goal: centers equal
        val pt = it.convertPoint(center, fromView = superview)
        it.setContentOffset(CGPointMake(
            pt.useContents { x }.minus(it.bounds.useContents { size.width / 2 }).takeIf { _ -> it.horizontal } ?: 0.0,
            pt.useContents { y }.minus(it.bounds.useContents { size.height / 2 }).takeIf { _ -> !it.horizontal } ?: 0.0,
        ), animated = animated)
    }
}
