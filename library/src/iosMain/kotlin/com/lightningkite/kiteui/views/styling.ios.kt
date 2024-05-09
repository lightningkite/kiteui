package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactiveScope
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.QuartzCore.*
import platform.UIKit.UIColor
import platform.UIKit.UIImageView
import platform.UIKit.UIView
import kotlin.math.min
import kotlin.time.DurationUnit
import platform.Foundation.*

fun Color.toUiColor(): UIColor = UIColor(
    red = red.toDouble().coerceIn(0.0, 1.0),
    green = green.toDouble().coerceIn(0.0, 1.0),
    blue = blue.toDouble().coerceIn(0.0, 1.0),
    alpha = alpha.toDouble().coerceIn(0.0, 1.0)
)

@OptIn(ExperimentalForeignApi::class)
fun ViewWriter.handleTheme(
    view: NView,
    viewDraws: Boolean = true,
    viewLoads: Boolean = false,
    background: (Theme) -> Unit = {},
    backgroundRemove: () -> Unit = {},
    foreground: (Theme) -> Unit = {},
    foregroundSkipAnimate: suspend (Theme) -> Unit = {},
    setup: () -> Unit,
) {
    val transition = transitionNextView
    transitionNextView = ViewWriter.TransitionNextView.No
    val currentTheme = currentTheme
    val isRoot = isRoot
    this.isRoot = false

    var firstTime = true
    inline fun animateAfterFirst(crossinline action: () -> Unit) {
        if (firstTime) {
            firstTime = false
            action()
        } else {
            view.animateIfAllowed(action)
        }
    }

    val viewForcePadding = view.extensionForcePadding
    val mightTransition = transition != ViewWriter.TransitionNextView.No
    val usePadding = viewForcePadding ?: (mightTransition && !isRoot)
    val parentSpacingCalc = lastSpacing

    var cancelAnimation: (() -> Unit)? = null
    if (viewLoads) calculationContext.onRemove { cancelAnimation?.invoke(); cancelAnimation = null }

    view.calculationContext.reactiveScope {
        val theme = currentTheme()
        view.extensionAnimationDuration = theme.transitionDuration.toDouble(DurationUnit.SECONDS)
        val shouldTransition = when (transition) {
            ViewWriter.TransitionNextView.No -> false
            ViewWriter.TransitionNextView.Yes -> true
            is ViewWriter.TransitionNextView.Maybe -> transition.logic()
        }
        val useBackground = shouldTransition

        if (usePadding) {
            view.extensionPadding = (view.spacingOverride?.await() ?: theme.spacing).value
        } else {
            view.extensionPadding = 0.0
        }

//        val loading = viewLoads && view.iosCalculationContext.loading.await()
        val loading = false

        val parentSpacing = parentSpacingCalc().value
        animateAfterFirst {
            if (loading) {
                applyThemeBackground(theme, view, parentSpacing)
                if (!useBackground) view.layer.apply {
                    shadowColor = null
                    shadowOpacity = 0f
                    shadowOffset = CGSizeMake(0.0, 0.0)
                    shadowRadius = 0.0
                }
                var current = false
                val animate = {
                    current = !current
                    view.layer.backgroundColor =
                        theme.background.closestColor().highlight(if (current) 0.15f else 0.05f).toUiColor().CGColor
                }
                var continueAnimate = { }
                continueAnimate = {
                    UIView.animateWithDuration(1.0, animate) { continueAnimate() }
                }
                UIView.animateWithDuration(1.0, animate) { continueAnimate() }
                cancelAnimation = {
                    continueAnimate = {}
                }
            } else {
                cancelAnimation?.invoke()
                cancelAnimation = null
                if (useBackground) {
                    applyThemeBackground(theme, view, parentSpacing)
                    background(theme)
                } else if(view is UIImageView) {
                    view.clearOldLayers()
                } else {
                    view.clearOldLayers()
                    backgroundRemove()
                }
            }
            foreground(theme)
        }
        foregroundSkipAnimate(theme)
    }

    lastSpacing = { view.spacingOverride?.await() ?: currentTheme().spacing }
    setup()
    lastSpacing = parentSpacingCalc
}

private fun NView.clearOldLayers() {
    layer.sublayers?.forEach {
        if(it is CAGradientLayerResizing) it.removeFromSuperlayer()
        if(it is CALayerResizing) it.removeFromSuperlayer()
    }
}
private inline fun <reified T: CALayer> NView.getOrCreateLayer(create: ()->T, setup: T.()->Unit): T {
    var sublayer: T? = null
    layer.sublayers?.forEach {
        if(it is T) {
            setup(it)
            sublayer = it
        } else if(it is CAGradientLayerResizing) it.removeFromSuperlayer()
        else if(it is CALayerResizing) it.removeFromSuperlayer()
    }
    return sublayer ?: run {
        val newLayer = create()
        setup(newLayer)
        layer.insertSublayer(newLayer, atIndex = 0.toUInt())
        newLayer
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun applyThemeBackground(
    theme: Theme,
    view: NView,
    parentSpacing: Double,
) {
    when (val b = theme.background) {
        is Color -> view.getOrCreateLayer(::CALayerResizing) {
            this.backgroundColor = b.toUiColor().CGColor!!
        }
        is LinearGradient -> view.getOrCreateLayer(::CAGradientLayerResizing) {
            this.type = kCAGradientLayerAxial
            this.locations = b.stops.map {
                NSNumber.numberWithFloat(it.ratio)
            }
            this.colors = b.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
            this.startPoint = CGPointMake(-b.angle.cos() * .5 + .5, -b.angle.sin() * .5 + .5)
            this.endPoint = CGPointMake(b.angle.cos() * .5 + .5, b.angle.sin() * .5 + .5)
        }
        is RadialGradient -> view.getOrCreateLayer(::CAGradientLayerResizing) {
            this.type = kCAGradientLayerRadial
            this.locations = b.stops.map {
                NSNumber.numberWithFloat(it.ratio)
            }
            this.colors = b.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
            this.startPoint = CGPointMake(0.5, 0.5)
            this.endPoint = CGPointMake(0.0, 0.0)
        }
    }.apply {
        zPosition = -99999.0
        (this as? CALayerResizing)?.parentSpacing = parentSpacing
        (this as? CAGradientLayerResizing)?.parentSpacing = parentSpacing

        borderWidth = theme.outlineWidth.value
        borderColor = theme.outline.closestColor().toUiColor().CGColor
        (this as? CALayerResizing)?.desiredCornerRadius = theme.cornerRadii
        (this as? CAGradientLayerResizing)?.desiredCornerRadius = theme.cornerRadii

        if (theme.elevation != 0.px) {
            shadowColor = UIColor.grayColor.CGColor
            shadowOpacity = 1f
            shadowOffset = CGSizeMake(0.0, theme.elevation.value)
            shadowRadius = theme.elevation.value
        }
    }.matchParentSize("insert")
}

@OptIn(ExperimentalForeignApi::class)
private inline fun CALayer.matchParentSize(context: String) {
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
        if(it is CALayerResizing) {
            it.matchParentSize("layoutSubviewsAndLayers")
            it.refreshCorners()
        } else if(it is CAGradientLayerResizing) {
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
        val v = when(val d = desiredCornerRadius) {
            is CornerRadii.Constant -> d.value.value.coerceAtMost(parentSpacing).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadii.ForceConstant -> d.value.value.coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadii.RatioOfSize -> d.ratio * bounds.useContents { min(size.width, size.height) }
            is CornerRadii.RatioOfSpacing -> parentSpacing.times(d.value).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
        }
        superlayer?.let { it.modelLayer() ?: it }?.cornerRadius = v
        cornerRadius = v
    }

    override fun layoutSublayers() {
        super.layoutSublayers()
        refreshCorners()
    }

    init {
        needsDisplayOnBoundsChange = true
    }
}

@OptIn(ExperimentalForeignApi::class)
class CALayerResizing: CALayer {

    @OverrideInit constructor():super()
    @OverrideInit constructor(coder: platform.Foundation.NSCoder):super(coder)
    @OverrideInit constructor(layer: kotlin.Any):super(layer)

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
        val v = when(val d = desiredCornerRadius) {
            is CornerRadii.Constant -> d.value.value.coerceAtMost(parentSpacing).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadii.ForceConstant -> d.value.value.coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
            is CornerRadii.RatioOfSize -> d.ratio * bounds.useContents { min(size.width, size.height) }
            is CornerRadii.RatioOfSpacing -> parentSpacing.times(d.value).coerceAtMost(bounds.useContents { min(size.width, size.height) / 2 })
        }
        superlayer?.let { it.modelLayer() ?: it }?.cornerRadius = v
        cornerRadius = v
    }

    override fun layoutSublayers() {
        super.layoutSublayers()
        refreshCorners()
    }

    init {
        needsDisplayOnBoundsChange = true
        frame = CGRectMake(0.0, 0.0, 10.0, 10.0)
        refreshCorners()
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