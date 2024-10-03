package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.QuartzCore.*
import platform.UIKit.UIColor
import platform.UIKit.UIView
import kotlin.math.min

fun Color.toUiColor(): UIColor = UIColor(
    red = red.toDouble().coerceIn(0.0, 1.0),
    green = green.toDouble().coerceIn(0.0, 1.0),
    blue = blue.toDouble().coerceIn(0.0, 1.0),
    alpha = alpha.toDouble().coerceIn(0.0, 1.0)
)


@OptIn(ExperimentalForeignApi::class)
internal inline fun CALayer.matchParentSize(context: String) {
    superlayer?.bounds?.let {
        frame = it
    }
}

internal inline fun UIView.layoutSubviewsAndLayers() {
    if(this != null) {
        layoutSubviews()
        layoutLayers()
    }
}

internal fun UIView.layoutLayers() {
    layer?.sublayers?.forEach {
        it as CALayer
        if(it is CAGradientLayerResizing) {
            it.matchParentSize("layoutSubviewsAndLayers")
            it.refreshCorners()
        }
    }
}
internal fun UIView.layoutLayers(parentSpacing: Double) {
    layer?.sublayers?.forEach {
        it as CALayer
        if(it is CAGradientLayerResizing) {
            it.parentSpacing = parentSpacing
            it.matchParentSize("layoutSubviewsAndLayers")
            it.refreshCorners()
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class CAGradientLayerResizing: CAGradientLayer {

    @OverrideInit constructor():super()
    @OverrideInit constructor(coder: platform.Foundation.NSCoder):super(coder)
    @OverrideInit constructor(layer: kotlin.Any):super(layer)

    private var backgroundMask: CALayer? = null

    /**
     * In some cases, we need a separate layer to mask views. The actual CAGradientLayerResizing layer cannot be used
     * because it has a superlayer and the CALayer mask property does not work with layers that have superlayers
     */
    fun getOrInitBackgroundMask(): CALayer {
        if (backgroundMask == null) {
            val whiteLayer = CALayer().apply {
                backgroundColor = UIColor.whiteColor.CGColor
                frame = this@CAGradientLayerResizing.frame
            }
            backgroundMask = whiteLayer
            refreshCorners()
        }
        return backgroundMask!!
    }

    var desiredCornerRadius: CornerRadiusOptions = CornerRadius.ForceConstant(0.px)
        set(value) {
            field = value
            refreshCorners()
        }
    var parentSpacing: CGFloat = 0.0
        set(value) {
            field = value
            refreshCorners()
        }

    fun refreshCorners() {
        fun CornerRadius.toRawValue() = when (this) {
            is CornerRadius.Constant -> value.value.coerceAtMost(parentSpacing).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadius.ForceConstant -> value.value.coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadius.RatioOfSize -> ratio * bounds.useContents { min(size.width, size.height) }
            is CornerRadius.RatioOfSpacing -> parentSpacing.times(value).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
        }
        val desiredCornerRadius = desiredCornerRadius
        val v: Double
        if (desiredCornerRadius is CornerRadius) {
            v = desiredCornerRadius.toRawValue()
            maskedCorners = kCALayerMinXMinYCorner or kCALayerMaxXMinYCorner or kCALayerMinXMaxYCorner or kCALayerMaxXMaxYCorner
        } else {
            // Due to UIKit limitations, this implementation works only when we are not trying to set different corners
            // to different non-zero radii
            desiredCornerRadius as CornerRadii
            val rawRadiusForEachCorner = mapOf(
                kCALayerMinXMinYCorner to desiredCornerRadius.topStart.toRawValue(),
                kCALayerMaxXMinYCorner to desiredCornerRadius.topEnd.toRawValue(),
                kCALayerMinXMaxYCorner to desiredCornerRadius.bottomStart.toRawValue(),
                kCALayerMaxXMaxYCorner to desiredCornerRadius.bottomEnd.toRawValue()
            )
            v = rawRadiusForEachCorner.firstNotNullOfOrNull { it.value.takeUnless { it == 0.0 } } ?: 0.0
            maskedCorners = rawRadiusForEachCorner.filter { it.value == v }.keys.reduce { a, b -> a or b }
        }
        superlayer?.let { it.modelLayer() ?: it }?.cornerRadius = v
        backgroundMask?.cornerRadius = v
        cornerRadius = v
    }

    override fun layoutSublayers() {
        super.layoutSublayers()
        backgroundMask?.frame = frame
        refreshCorners()
    }

    init {
        needsDisplayOnBoundsChange = true
    }
}

//100 	Thin (Hairline)
//200 	Extra Light (Ultra Light)
//300 	Light
//400 	Normal
//500 	Medium
//600 	Semi Bold (Demi Bold)
//700 	Bold
//800 	Extra Bold (Ultra Bold)
//900 	Black (Heavy)

//UIFontWeight light UIFontWeight(rawValue: -0.4000000059604645)
//UIFontWeight medium UIFontWeight(rawValue: 0.23000000417232513)
//UIFontWeight regular UIFontWeight(rawValue: 0.0)
//UIFontWeight semibold UIFontWeight(rawValue: 0.30000001192092896)
//UIFontWeight bold UIFontWeight(rawValue: 0.4000000059604645)
fun Int.toUIFontWeight(): Double {
    return (this - 400) * (0.4 / 300)
}