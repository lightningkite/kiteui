package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactiveScope
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIColor
import platform.UIKit.UIImageView
import platform.UIKit.UIView
import kotlin.math.min

fun Color.toUiColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble()
)

@OptIn(ExperimentalForeignApi::class)
fun ViewWriter.handleTheme(
    view: NView,
    viewDraws: Boolean = true,
    viewLoads: Boolean = false,
    background: (Theme) -> Unit = {},
    backgroundRemove: () -> Unit = {},
    foreground: (Theme) -> Unit = {}
) {
    val transition = transitionNextView
    transitionNextView = ViewWriter.TransitionNextView.No
    val currentTheme = currentTheme
    val parentTheme = lastTheme
    val isRoot = isRoot
    this.isRoot = false
    val changedThemes = changedThemes
    this.changedThemes = false

    var firstTime = true
    inline fun animateAfterFirst(crossinline action: () -> Unit) {
        if (firstTime) {
            firstTime = false
            action()
        } else {
            animateIfAllowed(action)
        }
    }

    var cancelAnimation: (() -> Unit)? = null
    if (viewLoads) calculationContext.onRemove { cancelAnimation?.invoke(); cancelAnimation = null }

    view.calculationContext.reactiveScope {
        val theme = currentTheme()

        val shouldTransition = when (transition) {
            ViewWriter.TransitionNextView.No -> false
            ViewWriter.TransitionNextView.Yes -> true
            is ViewWriter.TransitionNextView.Maybe -> transition.logic()
        }
        val mightTransition = transition != ViewWriter.TransitionNextView.No
        val useBackground = shouldTransition
        val usePadding = view.extensionForcePadding ?: (mightTransition && !isRoot)/* && view !is UIImageView*/

        if (usePadding) {
            view.extensionPadding = (view.spacingOverride?.await() ?: theme.spacing).value
        } else {
            view.extensionPadding = 0.0
        }

        val parentSpacing = if(isRoot) 0.0 else (view.superview?.spacingOverride?.await() ?: theme.spacing).value
        val loading = viewLoads && view.iosCalculationContext.loading.await()

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
                    val cr = when(val it = theme.cornerRadii) {
                        is CornerRadii.Constant -> min(parentSpacing, it.value.value)
                        is CornerRadii.RatioOfSpacing -> it.value * parentSpacing
                    }
                    view.layer.sublayers?.forEach { if(it is CAGradientLayerResizing) it.removeFromSuperlayer() }
                    view.layer.apply {
                        backgroundColor = null
                        borderWidth = 0.0
                        borderColor = null
                        cornerRadius = cr
                        shadowColor = null
                        shadowOpacity = 0f
                        shadowOffset = CGSizeMake(0.0, 0.0)
                        shadowRadius = 0.0
                    }
                } else {
                    view.layer.sublayers?.forEach { if(it is CAGradientLayerResizing) it.removeFromSuperlayer() }
                    view.layer.apply {
                        backgroundColor = null
                        borderWidth = 0.0
                        borderColor = null
                        cornerRadius = 0.0
                        shadowColor = null
                        shadowOpacity = 0f
                        shadowOffset = CGSizeMake(0.0, 0.0)
                        shadowRadius = 0.0
                    }
                    backgroundRemove()
                }
            }
            foreground(theme)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun applyThemeBackground(
    theme: Theme,
    view: NView,
    parentSpacing: Double,
    borders: Boolean
) {
    val cr = when(val it = theme.cornerRadii) {
        is CornerRadii.Constant -> min(parentSpacing, it.value.value)
        is CornerRadii.RatioOfSpacing -> it.value * parentSpacing
    }
    if (borders) {
        view.layer.apply {
            cornerRadius = cr
            shadowColor = UIColor.grayColor.CGColor
            shadowOpacity = 1f
            shadowOffset = CGSizeMake(0.0, theme.elevation.value)
            shadowRadius = theme.elevation.value
        }
    }
    when (val b = theme.background) {
        is Color -> {
            view.layer.apply {
                sublayers?.forEach { if(it is CAGradientLayerResizing) it.removeFromSuperlayer() }
                backgroundColor = b.toUiColor().CGColor
                if (borders) {
                    borderWidth = theme.outlineWidth.value
                    borderColor = theme.outline.closestColor().toUiColor().CGColor
                }
            }
        }

        is LinearGradient -> {
            view.layer.apply {
                borderWidth = 0.0
                sublayers?.forEach { if(it is CAGradientLayerResizing) it.removeFromSuperlayer() }
                insertSublayer(CAGradientLayerResizing().apply {
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
                        cornerRadius = cr
                    }
                }, atIndex = 0.toUInt())
            }
        }

        is RadialGradient -> {
            view.layer.apply {
                borderWidth = 0.0
                sublayers?.forEach { if(it is CAGradientLayerResizing) it.removeFromSuperlayer() }
                insertSublayer(CAGradientLayerResizing().apply {
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
                        cornerRadius = cr
                        shadowColor = UIColor.grayColor.CGColor
                        shadowOpacity = 1f
                        shadowOffset = CGSizeMake(0.0, theme.elevation.value)
                        shadowRadius = theme.elevation.value
                    }
                }, atIndex = 0.toUInt())
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class CAGradientLayerResizing: CAGradientLayer() {
    init {
        needsDisplayOnBoundsChange = true
        frame = CGRectMake(0.0, 0.0, 10.0, 10.0)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun drawInContext(ctx: CGContextRef?) {
        superlayer?.bounds?.let { frame = it }
        super.drawInContext(ctx)
    }
}