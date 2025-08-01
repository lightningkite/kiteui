package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.signal.CalculationContext
import com.lightningkite.signal.Property
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGAffineTransformRotate
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName
import kotlin.experimental.ExperimentalNativeApi
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.signal.invokeAllSafe
import platform.QuartzCore.CATransaction

public typealias NView = UIView


@Suppress("UNCHECKED_CAST")
public val UIView.spacingOverride: Property<Dimension?>?
    get() = (this as? UIViewWithSpacingRulesProtocol)
        ?.getSpacingOverrideProperty()
        ?.let { it as? Property<Dimension?> }
