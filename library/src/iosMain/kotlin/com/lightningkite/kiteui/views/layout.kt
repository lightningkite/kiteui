package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.views.direct.ScrollLayout
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
fun UIView.informParentOfSizeChange() {
    if (superview !is ScrollLayout) {
        (superview as? UIViewWithSizeOverridesProtocol)?.subviewDidChangeSizing(this)
    } else {
        superview?.informParentOfSizeChange()
    }
    setNeedsLayout()
}