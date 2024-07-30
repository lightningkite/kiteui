package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
actual class Space actual constructor(context: RContext, multiplier: Double): RView(context) {
    override val native = NSpace()
    init {
        sizeConstraints = SizeConstraints(
            width = theme.spacing * multiplier,
            height = theme.spacing * multiplier
        )
    }
}
@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NSpace(): UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = CGSizeMake(0.0, 0.0)
}