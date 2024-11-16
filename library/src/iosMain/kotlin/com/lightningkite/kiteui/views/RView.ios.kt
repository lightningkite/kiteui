package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.reactive.AppState
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIColor
import platform.UIKit.UIView
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.PI
import kotlin.math.sin
import kotlin.native.ref.WeakReference
import kotlin.time.DurationUnit


actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: UIView
    var tag: Any? = null

    actual override var showOnPrint: Boolean = true

    var sizeConstraints: SizeConstraints?
        get() = native.extensionSizeConstraints
        set(value) {
            native.extensionSizeConstraints = value
        }

    protected actual override fun opacitySet(value: Double) {
        animateIfAllowed {
            native.alpha = value
        }
    }

    protected actual override fun existsSet(value: Boolean) {
        native.hidden = !value
        if (fullyStarted) {
            native.informParentOfSizeChange()
        }
//        if (animationsEnabled) {
//            UIView.animateWithDuration(theme.transitionDuration.toDouble(DurationUnit.SECONDS)) {
//                native.hidden = !value
//                if(fullyStarted) {
//                    native.informParentOfSizeChange()
//                    native.superview?.layoutIfNeeded()
//                }
//            }
//        } else {
//            native.hidden = !value
//            if(fullyStarted) {
//                native.informParentOfSizeChange()
//            }
//        }
    }

    protected actual override fun visibleSet(value: Boolean) {
        animateIfAllowed {
            native.alpha = if (value) 1.0 else 0.0
        }
    }

    private val mySpacing get() = (spacing ?: if (useNavSpacing) theme.navSpacing else theme.spacing)
    protected actual override fun spacingSet(value: Dimension?) {
        native.spacingOverride?.value = value
        val spacing = mySpacing.value
        for (child in children) {
            child.native.layoutLayers(spacing)
        }
    }

    protected actual override fun ignoreInteractionSet(value: Boolean) {
//        if (value) {
//            val actionHolder = object : NSObject() {
//                @ObjCAction
//                fun eventHandler() {
//                }
//            }
//            val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
//            native.addGestureRecognizer(rec)
//            onRemove {
//                // Retain the sleeve until disposed
//                rec.enabled
//                actionHolder.description
//            }
//        }
    }

    protected actual override fun forcePaddingSet(value: Boolean?) {
        native.extensionForcePadding = value
    }

    actual override fun screenRectangle(): Rect? {
        val windowView = native.window?.rootViewController?.view ?: return null
        val parent = native.superview ?: return null
        return windowView.convertRect(native.frame, fromView = parent).useContents {
            Rect(
                left = (origin.x),
                right = (origin.x + size.width),
                top = (origin.y),
                bottom = (origin.y + size.height),
            )
        }
    }

    actual override fun scrollIntoView(
        horizontal: Align?,
        vertical: Align?,
        animate: Boolean
    ) {
        afterTimeout(16) {
            native.scrollToMe(animate)
        }
    }

    actual override fun requestFocus() {
        afterTimeout(16) {
            native.becomeFirstResponder()
        }
    }

    actual override fun applyElevation(dimension: Dimension) {
        if (dimension.value == 0.0) native.layer.apply {
            shadowColor = null
            shadowOpacity = 0f
            shadowOffset = CGSizeMake(0.0, 0.0)
            shadowRadius = 0.0
        } else native.layer.apply {
            shadowColor = UIColor.grayColor.CGColor
            shadowOpacity = 1f
            shadowOffset = CGSizeMake(0.0, dimension.value)
            shadowRadius = dimension.value
        }
    }

    actual override fun applyPadding(dimension: Dimension?) {
        native.extensionPadding = dimension?.value
    }

    var previousLoadAnimationHandle: (() -> Unit)? = null
    var backgroundLayer: CAGradientLayerResizing? = null
    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        animateIfAllowed {
//            native.clearOldLayers()
//            if(fullyApply) applyThemeBackground(theme, native, parent?.mySpacing ?: theme.spacing)
//            native.layoutLayers()
            if (!fullyApply) {
                backgroundLayer?.removeFromSuperlayer()
                backgroundLayer = null
            }
            val layer = backgroundLayer ?: run {
                val newLayer = CAGradientLayerResizing()
                backgroundLayer = newLayer
                native.layer.insertSublayer(newLayer, atIndex = 0.toUInt())
                newLayer
            }
            previousLoadAnimationHandle?.invoke()
            previousLoadAnimationHandle = null
            with(layer) {
                if (fullyApply) {
                    when (val b = theme.background) {
                        is Color -> {
                            val c = b.toUiColor().CGColor!!
                            this.type = kCAGradientLayerAxial
                            this.locations = listOf(NSNumber.numberWithFloat(0f), NSNumber.numberWithFloat(1f))
                            this.colors = listOf(c, c).map { it.toObjcId() }
                            this.startPoint = CGPointMake(0.0, 0.0)
                            this.endPoint = CGPointMake(1.0, 1.0)
                        }

                        is FadingColor -> {
                            val c = b.base.toUiColor().CGColor!!
                            this.type = kCAGradientLayerAxial
                            this.locations = listOf(NSNumber.numberWithFloat(0f), NSNumber.numberWithFloat(1f))
                            this.colors = listOf(c, c).map { it.toObjcId() }
                            this.startPoint = CGPointMake(0.0, 0.0)
                            this.endPoint = CGPointMake(1.0, 1.0)
                            previousLoadAnimationHandle = AppState.animationFrame.addListener {
                                val i = Color.interpolate(b.base, b.alternate, (sin(clockMillis() / 2000.0 * PI * 2) / 2 + 0.5).toFloat()).toUiColor().CGColor!!
                                this.colors = listOf(i, i).map { it.toObjcId() }
                            }
                        }

                        is LinearGradient -> {
                            this.type = kCAGradientLayerAxial
                            this.locations = b.stops.map {
                                NSNumber.numberWithFloat(it.ratio)
                            }
                            this.colors = b.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
                            this.startPoint = CGPointMake(-b.angle.cos() * .5 + .5, -b.angle.sin() * .5 + .5)
                            this.endPoint = CGPointMake(b.angle.cos() * .5 + .5, b.angle.sin() * .5 + .5)
                        }

                        is RadialGradient -> {
                            this.type = kCAGradientLayerRadial
                            this.locations = b.stops.map {
                                NSNumber.numberWithFloat(it.ratio)
                            }
                            this.colors = b.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
                            this.startPoint = CGPointMake(0.5, 0.5)
                            this.endPoint = CGPointMake(0.0, 0.0)
                        }
                    }
                    borderWidth = theme.outlineWidth.value
                    borderColor = theme.outline.closestColor().toUiColor().CGColor
                }

                zPosition = -99999.0
                parentSpacing = this@RView.parentSpacing.value
                desiredCornerRadius = theme.cornerRadii

                matchParentSize("insert")
            }
        }
    }

    @OptIn(ExperimentalNativeApi::class)
    override fun leakDetect() {
        super.leakDetect()
        WeakReference(native).checkLeakAfterDelay(1_000)
    }

    override fun postSetup() {
        super.postSetup()
        ObjCountTrackers.track(this)
        ObjCountTrackers.track(native)
    }

    actual override fun applyForeground(theme: Theme) {
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        if (index == native.subviews.size)
            native.addSubview(view.native)
        else
            native.insertSubview(view.native, index.toLong())
        if (children[index].native != native.subviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.subviews}")
    }

    actual override fun internalRemoveChild(index: Int) {
        if (children[index].native != native.subviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.subviews}")
        if (index >= native.subviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${native.subviews.size}")
        }
        (native.subviews[index] as UIView).removeFromSuperview()
    }

    actual override fun internalClearChildren() {
        native.subviews.toList().forEach {
            (it as UIView).let {
                it.removeFromSuperview()
            }
        }
    }
}

var animationsEnabled: Boolean = true
actual inline fun RView.withoutAnimation(action: () -> Unit) {
    native.withoutAnimation(action)
}

inline fun UIView.withoutAnimation(action: () -> Unit) {
    try {
        animationsEnabled = false
        CATransaction.begin()
        CATransaction.disableActions()
        try {
            action()
        } finally {
            CATransaction.commit()
        }
    } finally {
        animationsEnabled = true
    }
}

inline fun UIView.animateIfAllowed(crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(/*extensionAnimationDuration ?:*/ 0.5) {
        action()
    } else {
        action()
    }
}

inline fun RView.animateIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { onComplete() },
        animations = { action() }
    ) else {
        action()
        onComplete()
    }
}
