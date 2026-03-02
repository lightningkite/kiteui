package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIProgressView
import platform.UIKit.UIView


actual class CircularProgress actual constructor(context: RContext) : RView(context) {
    override val native = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    actual var ratio: Float = 0f

    // by Claude
    override var accessibilityValue: String?
        get() = ratio.toString()
        set(value) { super.accessibilityValue = value }

    // by Claude
    override val accessibilityType: String get() = "CircularProgress"
}
