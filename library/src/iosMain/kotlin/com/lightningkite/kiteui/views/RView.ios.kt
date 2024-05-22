package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIColor
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.objc.sel_registerName
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

@OptIn(ExperimentalForeignApi::class)
actual abstract class RView(context: RContext) : RViewHelper(context) {
    abstract val native: UIView

    protected actual override fun opacitySet(value: Double) {
        native.animateIfAllowed {
            native.alpha = value
        }
    }

    protected actual override fun existsSet(value: Boolean) {
        if (animationsEnabled) {
            UIView.animateWithDuration(theme.transitionDuration.toDouble(DurationUnit.SECONDS)) {
                native.hidden = !value
                native.informParentOfSizeChange()
                native.superview?.layoutIfNeeded()
            }
        } else {
            native.hidden = !value
            native.informParentOfSizeChange()
        }
    }

    protected actual override fun visibleSet(value: Boolean) {
        native.animateIfAllowed {
            native.alpha = if (value) 1.0 else 0.0
        }
    }

    protected actual override fun spacingSet(value: Dimension?) {
        native.spacingOverride?.value = value
        if(useBackground != UseBackground.No) {
            native.extensionPadding = value?.value
        }
    }

    protected actual override fun ignoreInteractionSet(value: Boolean) {
        if(value) {
            val actionHolder = object : NSObject() {
                @ObjCAction
                fun eventHandler() {
                }
            }
            val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
            native.addGestureRecognizer(rec)
            calculationContext.onRemove {
                // Retain the sleeve until disposed
                rec.enabled
                actionHolder.description
            }
        }
    }

    protected actual override fun forcePaddingSet(value: Boolean?) {
        native.extensionForcePadding = value
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
        if(dimension.value == 0.0) native.layer.apply {
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

    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        native.animateIfAllowed {
            native.clearOldLayers()
            if(fullyApply) applyThemeBackground(theme, native, parent?.spacing?.value ?: 0.0)
        }
    }

    actual override fun applyForeground(theme: Theme) {
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        native.insertSubview(view.native, index.toLong())
    }

    actual override fun internalRemoveChild(index: Int) {
        (native.subviews.getOrNull(index) as? UIView)?.removeFromSuperview()
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
    if (!animationsEnabled) {
        CATransaction.begin()
        CATransaction.disableActions()
        try {
            action()
        } finally {
            CATransaction.commit()
        }
        return
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

inline fun NView.animateIfAllowed(crossinline animations: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(extensionAnimationDuration ?: 0.15) {
        animations()
    } else {
        animations()
    }
}

inline fun NView.animateIfAllowedWithComplete(crossinline animations: () -> Unit, crossinline completion: () -> Unit, ) {
    if (animationsEnabled) UIView.animateWithDuration(duration = extensionAnimationDuration ?: 0.15, animations = {
        animations()
    }, completion = { completion() }) else {
        animations()
    }
}