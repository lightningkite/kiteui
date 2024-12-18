package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.RView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.QuartzCore.*
import platform.UIKit.UIColor
import platform.UIKit.UIImageView
import platform.UIKit.UIView
import platform.UIKit.UIScrollView
import kotlin.math.min
import kotlin.time.DurationUnit
import platform.Foundation.*

fun Color.toUiColor(): UIColor = UIColor(
    red = red.toDouble().coerceIn(0.0, 1.0),
    green = green.toDouble().coerceIn(0.0, 1.0),
    blue = blue.toDouble().coerceIn(0.0, 1.0),
    alpha = alpha.toDouble().coerceIn(0.0, 1.0)
)


@Suppress("NOTHING_TO_INLINE")
internal inline fun UIView.layoutSubviewsAndLayers() {
    // Fixes a really cursed crash where "this" is null due to GC interactions
    @Suppress("SENSELESS_COMPARISON")
    if (this != null) {
        layoutSubviews()
        layoutLayers()
    }
}

internal fun UIView.layerSize(): CValue<CGRect> {
    val n = this
    val bounds = if(n is UIScrollView) n.contentSize.useContents {
        val cs = this
        n.bounds.useContents {
            val b = this
            CGRectMake(0.0, 0.0, cs.width + b.size.width, cs.height + b.size.height)
        }
    } else n.bounds
    return bounds
}

internal fun UIView.layoutLayers() {
    val bounds = layerSize()
    layer?.sublayers?.forEach {
        it as CALayer
        if (it is CAGradientLayerResizing) {
            it.frame = bounds
            it.refreshCorners()
        }
    }
}

internal fun UIView.layoutLayers(parentSpacing: Double) {
    val bounds = layerSize()
    layer?.sublayers?.forEach {
        it as CALayer
        if (it is CAGradientLayerResizing) {
            it.parentSpacing = parentSpacing
            it.frame = bounds
            it.refreshCorners()
        }
    }
}


class CAGradientLayerResizing : CAGradientLayer {

    @OverrideInit
    constructor() : super()
    @OverrideInit
    constructor(coder: platform.Foundation.NSCoder) : super(coder)
    @OverrideInit
    constructor(layer: kotlin.Any) : super(layer)

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

    var desiredCornerRadius: CornerRadii = CornerRadii.ForceConstant(0.px)
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
        val v = when (val d = desiredCornerRadius) {
            is CornerRadii.Constant -> d.value.value.coerceAtMost(parentSpacing).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadii.ForceConstant -> d.value.value.coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadii.RatioOfSize -> d.ratio * bounds.useContents { min(size.width, size.height) }
            is CornerRadii.RatioOfSpacing -> parentSpacing.times(d.value).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            // TODO: Implement per-corner radii on iOS
            is CornerRadii.PerCorner -> 0.0
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