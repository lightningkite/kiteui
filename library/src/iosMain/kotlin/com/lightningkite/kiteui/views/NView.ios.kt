package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGAffineTransformRotate
import platform.QuartzCore.CATransaction
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName

typealias NView = UIView


@Suppress("UNCHECKED_CAST")
val UIView.spacingOverride: Signal<Dimension?>?
    get() = (this as? UIViewWithSpacingRulesProtocol)
        ?.getSpacingOverrideProperty()
        ?.let { it as? Signal<Dimension?> }
