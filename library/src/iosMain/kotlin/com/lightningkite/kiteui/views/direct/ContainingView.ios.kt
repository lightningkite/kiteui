package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.handleTheme
import platform.CoreGraphics.CGFloat
import platform.UIKit.UIView
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import kotlinx.cinterop.ExperimentalForeignApi

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NContainingView = UIView

@ViewDsl
actual inline fun ViewWriter.stackActual(crossinline setup: ContainingView.() -> Unit): Unit = element(FrameLayout()) {
    handleTheme(this, viewDraws = false)
    setup(ContainingView(this))
}

@ViewDsl
actual inline fun ViewWriter.colActual(crossinline setup: ContainingView.() -> Unit): Unit = element(LinearLayout()) {
    horizontal = false
    handleTheme(this, viewDraws = false) {
        gap = (spacingOverride.value ?: it.spacing).value
    }
    setup(ContainingView(this))
}

@ViewDsl
actual inline fun ViewWriter.rowActual(crossinline setup: ContainingView.() -> Unit): Unit = element(LinearLayout()) {
    horizontal = true
    handleTheme(this, viewDraws = false) {
        gap = (spacingOverride.value ?: it.spacing).value
    }
    setup(ContainingView(this))
}

actual var ContainingView.vertical: Boolean
    get() = (native as? LinearLayout)?.horizontal?.not() ?: false
    set(value) {
        (native as? LinearLayout)?.horizontal = !value
    }
