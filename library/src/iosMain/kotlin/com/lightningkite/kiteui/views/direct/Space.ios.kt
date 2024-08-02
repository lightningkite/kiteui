package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
actual class Space actual constructor(context: RContext, private val multiplier: Double): RView(context) {
    override val native = NSpace()
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.natSize = CGSizeMake(theme.spacing.value * multiplier, theme.spacing.value * multiplier)
    }
    init {
//        sizeConstraints =
    }
}
@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NSpace(var natSize: CValue<CGSize> = CGSizeMake(0.0, 0.0)): UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = natSize
}

//@ViewDsl
//actual inline fun ViewWriter.spaceActual(crossinline setup: Space.() -> Unit): Unit = element(NSpace()) {
//    handleTheme(
//        this,
//        foreground = {
//            extensionSizeConstraints = SizeConstraints(
//                minHeight = it.spacing,
//                minWidth = it.spacing
//            )
//        },
//    ) {
//        setup(Space(this))
//    }
//}
//
//actual fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit): Unit = element(NSpace()) {
//    handleTheme(
//        this,
//        foreground = {
//            extensionSizeConstraints = SizeConstraints(
//                minHeight = it.spacing * multiplier,
//                minWidth = it.spacing * multiplier
//            )
//        },
//    ) {
//        setup(Space(this))
//    }
//}