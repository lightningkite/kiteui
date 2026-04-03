package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*

import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView



actual class Space actual constructor(context: ElementContext, private val multiplier: Double): NativeElement(context) {
    override val native = NSpace()

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.natSize = CGSizeMake(theme.theme.gap.value * multiplier, theme.theme.gap.value * multiplier)
    }
}

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
//                minHeight = it.gap,
//                minWidth = it.gap
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
//                minHeight = it.gap * multiplier,
//                minWidth = it.gap * multiplier
//            )
//        },
//    ) {
//        setup(Space(this))
//    }
//}