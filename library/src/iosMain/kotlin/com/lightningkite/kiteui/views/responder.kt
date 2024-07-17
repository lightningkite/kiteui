package com.lightningkite.kiteui.views

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.UIKit.UIScrollView
import platform.UIKit.UIView

fun UIView.findFirstResponderChild(): UIView? {
    if(isFirstResponder) return this
    else return subviews.asSequence().mapNotNull { (it as UIView).findFirstResponderChild() }.firstOrNull()
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.scrollToMe(animated: Boolean = false) {
    scrollRectToVisibleClimb(
        x = this.frame.useContents { origin.x },
        y = this.frame.useContents { origin.y },
        width = this.frame.useContents { size.width },
        height = this.frame.useContents { size.height },
        animated = animated
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun UIView.scrollRectToVisibleClimb(x: Double, y: Double, width: Double, height: Double, animated: Boolean) {
    if(this is UIScrollView) {
        val mySize = frame.useContents { size.width to size.height }
        val contentSize = contentSize.useContents { width to height }
        setContentOffset(CGPointMake(
            x = (x + (width - mySize.first) / 2).coerceIn(0.0, (contentSize.first - mySize.first).coerceAtLeast(0.01)),
            y = (y + (height - mySize.second) / 2).coerceIn(0.0, (contentSize.second - mySize.second).coerceAtLeast(0.01)),
        ), animated = animated)
    } else superview?.scrollRectToVisibleClimb(
        x = x + this.frame.useContents { origin.x },
        y = y + this.frame.useContents { origin.y },
        width = width + this.frame.useContents { size.width },
        height = height + this.frame.useContents { size.height },
        animated = animated
    )
}