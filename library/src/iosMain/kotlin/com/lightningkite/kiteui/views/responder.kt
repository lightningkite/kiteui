package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.direct.ScrollLayout
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
    generateSequence(superview) { it.superview }.filterIsInstance<ScrollLayout>().firstOrNull()?.let {
        // goal: centers equal
        val pt = it.convertPoint(center, fromView = superview)
        println("Need to scroll to focus on ${pt.useContents { "$x, $y" }}")
        println("Scroll bounds size ${it.bounds.useContents { "${origin.x} ${origin.y} ${size.width} ${size.height}" }}")
        it.setContentOffset(CGPointMake(
            pt.useContents { x }.minus(it.bounds.useContents { size.width / 2 }).takeIf { _ -> it.horizontal } ?: 0.0,
            pt.useContents { y }.minus(it.bounds.useContents { size.height / 2 }).takeIf { _ -> !it.horizontal } ?: 0.0,
        ).also { println("Setting content offset to ${it.useContents { "$x, $y" }}") }, animated = animated)
    }
}
