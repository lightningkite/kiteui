package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
fun UIView.informParentOfSizeChange() {
//    println("informParentOfSizeChange $this")
    (superview as? UIViewWithSizeOverridesProtocol)?.subviewDidChangeSizing(this) ?: superview?.informParentOfSizeChangeDueToChild()
    setNeedsLayout()
}
@OptIn(ExperimentalForeignApi::class)
fun UIView.informParentOfSizeChangeDueToChild() {
    (superview as? UIViewWithSizeOverridesProtocol)?.subviewDidChangeSizing(this) ?: superview?.informParentOfSizeChangeDueToChild()
    setNeedsLayout()
}