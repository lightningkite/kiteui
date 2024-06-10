package com.lightningkite.kiteui.views.direct

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIScrollView
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
fun UIView.setPsuedoframe(x: CGFloat, y: CGFloat, width: CGFloat, height: CGFloat) {
//    if(frame.useContents {
//            this.origin.x == x && this.origin.y == y && this.size.width == width && this.size.height == height
//    }) return // No reason to trigger a bunch of stuff if we don't have to.
    if(this is UIScrollView) {
        setFrame(CGRectMake(x, y, width, height))
    } else {
        setBounds(
            CGRectMake(
                x = 0.0,
                y = 0.0,
                width = width,
                height = height,
            )
        )
        center = CGPointMake(x + width / 2, y + height / 2)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.setPsuedoframe(value: CValue<CGRect>) {
//    setFrame(value)
    setBounds(
        CGRectMake(
            x = 0.0,
            y = 0.0,
            width = value.useContents { size.width },
            height = value.useContents { size.height },
        )
    )
    center = value.useContents { CGPointMake(origin.x + size.width / 2, origin.y + size.height / 2)  }
}