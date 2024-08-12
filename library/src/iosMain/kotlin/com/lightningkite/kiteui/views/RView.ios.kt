package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import kotlinx.cinterop.ExperimentalForeignApi
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
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalForeignApi::class)
actual abstract class RView(context: RContext) : RViewHelper(context) {
    abstract val native: UIView
    var tag: Any? = null

    var sizeConstraints: SizeConstraints?
        get() = native.extensionSizeConstraints
        set(value) {
            native.extensionSizeConstraints = value
        }

    protected actual override fun opacitySet(value: Double) {
        native.animateIfAllowed {
            native.alpha = value
        }
    }

    protected actual override fun existsSet(value: Boolean) {
        native.hidden = !value
        if(fullyStarted) {
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
        native.animateIfAllowed {
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

    var backgroundLayer: CAGradientLayerResizing? = null
    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        native.animateIfAllowed {
//            native.clearOldLayers()
//            if(fullyApply) applyThemeBackground(theme, native, parent?.mySpacing ?: theme.spacing)
//            native.layoutLayers()
            if (!fullyApply) {
                backgroundLayer?.removeFromSuperlayer()
                backgroundLayer = null
            } else {
                val layer = backgroundLayer ?: run {
                    val newLayer = CAGradientLayerResizing()
                    backgroundLayer = newLayer
                    native.layer.insertSublayer(newLayer, atIndex = 0.toUInt())
                    newLayer
                }
                with(layer) {
                    when (val b = theme.background) {
                        is Color -> {
                            val c = b.toUiColor().CGColor!!
                            this.type = kCAGradientLayerAxial
                            this.locations = listOf(NSNumber.numberWithFloat(0f), NSNumber.numberWithFloat(1f))
                            this.colors = listOf(c, c).map { it.toObjcId() }
                            this.startPoint = CGPointMake(0.0, 0.0)
                            this.endPoint = CGPointMake(1.0, 1.0)
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
                    zPosition = -99999.0
                    parentSpacing = (parent?.mySpacing ?: theme.spacing).value

                    borderWidth = theme.outlineWidth.value
                    borderColor = theme.outline.closestColor().toUiColor().CGColor
                    desiredCornerRadius = theme.cornerRadii

                    matchParentSize("insert")
                }
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
        if(index == native.subviews.size)
            native.addSubview(view.native)
        else
            native.insertSubview(view.native, index.toLong())
        if(children[index].native != native.subviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.subviews}")
    }

    actual override fun internalRemoveChild(index: Int) {
        if(children[index].native != native.subviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.subviews}")
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
//    if (!animationsEnabled) {
//        CATransaction.begin()
//        CATransaction.disableActions()
//        try {
//            action()
//        } finally {
//            CATransaction.commit()
//        }
//        return
//    }
//    try {
//        animationsEnabled = false
//        CATransaction.begin()
//        CATransaction.disableActions()
//        try {
//            action()
//        } finally {
//            CATransaction.commit()
//        }
//    } finally {
//        animationsEnabled = true
//    }
}

inline fun UIView.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) {
        action()
    }
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
    if (animationsEnabled) UIView.animateWithDuration(/*extensionAnimationDuration ?:*/ 0.15) {
        action()
    } else {
        action()
    }
}