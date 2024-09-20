package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView


fun UIView.informParentOfSizeChange() {
//    println("informParentOfSizeChange $this")
    (superview as? UIViewWithSizeOverridesProtocol)?.subviewDidChangeSizing(this) ?: superview?.informParentOfSizeChangeDueToChild()
    setNeedsLayout()
}

fun UIView.informParentOfSizeChangeDueToChild() {
    (superview as? UIViewWithSizeOverridesProtocol)?.subviewDidChangeSizing(this) ?: superview?.informParentOfSizeChangeDueToChild()
    setNeedsLayout()
}