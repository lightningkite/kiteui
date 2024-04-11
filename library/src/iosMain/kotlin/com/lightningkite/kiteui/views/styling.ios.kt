package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactiveScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIColor
import platform.UIKit.UIImageView
import platform.UIKit.UIView
import kotlin.math.min
import kotlin.time.DurationUnit

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

    if (usePadding) {
        val hp = view.spacingOverride
        lastSpacing = { hp?.await() ?: currentTheme().spacing }
    }

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

        val loading = viewLoads && view.iosCalculationContext.loading.await()

        val parentSpacing = parentSpacingCalc().value
        animateAfterFirst {
            if (loading) {
                applyThemeBackground(theme, view, parentSpacing, true)
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
                    applyThemeBackground(theme, view, parentSpacing, true)
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
    }

    setup()

    if(usePadding) {
        lastSpacing = parentSpacingCalc
    }
}

private fun NView.clearOldLayers() {
    layer.sublayers?.forEach {
        if(it is CAGradientLayerResizing) it.removeFromSuperlayer()
        if(it is CALayerResizing) it.removeFromSuperlayer()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun applyThemeBackground(
    theme: Theme,
    view: NView,
    parentSpacing: Double,
    borders: Boolean
) {
    view.clearOldLayers()
    val newLayer = when (val b = theme.background) {
        is Color -> CALayerResizing().apply {
            this.parentSpacing = parentSpacing
            this.backgroundColor = b.toUiColor().CGColor!!
            this.needsDisplayOnBoundsChange = true

            if (borders) {
                borderWidth = theme.outlineWidth.value
                borderColor = theme.outline.closestColor().toUiColor().CGColor
                desiredCornerRadius = theme.cornerRadii
                shadowColor = UIColor.grayColor.CGColor
                shadowOpacity = 1f
                shadowOffset = CGSizeMake(0.0, theme.elevation.value)
                shadowRadius = theme.elevation.value
            }
        }
        is LinearGradient -> CAGradientLayerResizing().apply {
            this.parentSpacing = parentSpacing
            this.type = kCAGradientLayerAxial
            this.locations = b.stops.map {
                NSNumber.numberWithFloat(it.ratio)
            }
            this.colors = b.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
            this.startPoint = CGPointMake(-b.angle.cos() * .5 + .5, -b.angle.sin() * .5 + .5)
            this.endPoint = CGPointMake(b.angle.cos() * .5 + .5, b.angle.sin() * .5 + .5)
            this.needsDisplayOnBoundsChange = true

            if (borders) {
                borderWidth = theme.outlineWidth.value
                borderColor = theme.outline.closestColor().toUiColor().CGColor
                desiredCornerRadius = theme.cornerRadii
                shadowColor = UIColor.grayColor.CGColor
                shadowOpacity = 1f
                shadowOffset = CGSizeMake(0.0, theme.elevation.value)
                shadowRadius = theme.elevation.value
            }
        }
        is RadialGradient -> CAGradientLayerResizing().apply {
            this.parentSpacing = parentSpacing
            this.type = kCAGradientLayerRadial
            this.locations = b.stops.map {
                NSNumber.numberWithFloat(it.ratio)
            }
            this.colors = b.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
            this.startPoint = CGPointMake(0.5, 0.5)
            this.endPoint = CGPointMake(0.0, 0.0)
            this.needsDisplayOnBoundsChange = true
            if(borders) {
                borderWidth = theme.outlineWidth.value
                borderColor = theme.outline.closestColor().toUiColor().CGColor
                desiredCornerRadius = theme.cornerRadii
                shadowColor = UIColor.grayColor.CGColor
                shadowOpacity = 1f
                shadowOffset = CGSizeMake(0.0, theme.elevation.value)
                shadowRadius = theme.elevation.value
            }
        }
    }
    view.layer.insertSublayer(newLayer, atIndex = 0.toUInt())
}

@OptIn(ExperimentalForeignApi::class)
class CAGradientLayerResizing: CAGradientLayer() {
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
        superlayer?.cornerRadius = v
        cornerRadius = v
    }

    init {
        needsDisplayOnBoundsChange = true
        frame = CGRectMake(0.0, 0.0, 10.0, 10.0)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun drawInContext(ctx: CGContextRef?) {
        superlayer?.bounds?.let { frame = it }
        refreshCorners()
        super.drawInContext(ctx)
    }
}

@OptIn(ExperimentalForeignApi::class)
class CALayerResizing: CALayer() {
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
        superlayer?.cornerRadius = v
        cornerRadius = v
    }

    init {
        needsDisplayOnBoundsChange = true
        frame = CGRectMake(0.0, 0.0, 10.0, 10.0)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun drawInContext(ctx: CGContextRef?) {
        superlayer?.bounds?.let { frame = it }
        refreshCorners()
        super.drawInContext(ctx)
    }
}