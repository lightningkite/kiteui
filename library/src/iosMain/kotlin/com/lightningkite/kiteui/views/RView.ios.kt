package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.WrapperView
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIVibrancyEffect
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionTransitionCrossDissolve
import platform.UIKit.UIVisualEffectView
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.native.ref.WeakReference
import kotlin.time.DurationUnit


public actual abstract class RView public actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: UIView
    var tag: Any? = null

    public actual override var showOnPrint: Boolean = true

    var sizeConstraints: SizeConstraints?
        get() = native.extensionSizeConstraints
        set(value) {
            native.extensionSizeConstraints = value
        }

    override var opacity: Double
        get() = super.opacity
        set(value) {
            super.opacity = value
            animateIfAllowed {
                native.alpha = value
            }
        }

    override var shown: Boolean
        get() = super.shown
        set(value) {
            super.shown = value
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

    override var visible: Boolean
        get() = super.visible
        set(value) {
            super.visible = value
            animateIfAllowed {
                native.alpha = if (value) 1.0 else 0.0
            }
        }

    private val mySpacing get() = (gap ?: theme.gap)
    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.spacingOverride?.value = value
            val gap = mySpacing.value
            for (child in children) {
                child.native.layoutLayers(gap)
            }
        }

    override var ignoreInteraction: Boolean
        get() = super.ignoreInteraction
        set(value) {
            super.ignoreInteraction = value
            native.extensionIgnoreInteraction = value
        }

    // Update padding based on safe insets
    override fun refreshPadding() {
        val value = appliedPadding
        native.extensionPadding = value
        native.informParentOfSizeChange()
        val gap = max(mySpacing.value, padding?.value ?: 0.0)
        for (child in children) {
            child.native.layoutLayers(gap)
        }
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

    public actual override fun scrollIntoView(
        horizontal: Align?,
        vertical: Align?,
        animate: Boolean
    ) {
        afterTimeout(16) {
            native.scrollToMe(animate)
        }
    }

    public actual override fun requestFocus() {
        afterTimeout(16) {
            val n = native
            if (n is WrapperView) {
                (n.subviews.firstOrNull() as? UIView)?.becomeFirstResponder()
            } else {
                n.becomeFirstResponder()
            }
        }
    }


    // drag 'n drop
    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            // TODO
        }
    override var dropTargetDelegate: DropTargetDelegate?
        get() = super.dropTargetDelegate
        set(value) {
            super.dropTargetDelegate = value
            // TODO
        }


    protected var previousLoadAnimationHandle: (() -> Unit)? = null
    protected var backgroundLayer: CAGradientLayerResizing? = null

    /**
     * No matter how we set the zPosition or the "at" argument of the insertSublayer call, layers always cover the
     * content of UIImageView elements. Thus, we require a method of disabling the KiteUI background drawing entirely
     * for subclasses of RView. In this way, themes with a back may be applied so that corner radius is respected
     * without drawing anything that would cover the content of the view.
     */
    protected open val disableBackground = false

    class BlurBackgroundView: UIVisualEffectView(UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular))
    var effectBackground: BlurBackgroundView? = null

    actual override fun applyTheme(theme: ThemeAndBack) {
        if (theme.drawBackground && theme.theme.elevation.value != 0.0) native.layer.apply {
            val v = theme.theme.elevation.value
            shadowColor = UIColor.grayColor.CGColor
            shadowOpacity = 1f
            shadowOffset = CGSizeMake(0.0, v)
            shadowRadius = v
        }
        else native.layer.apply {
            shadowColor = null
            shadowOpacity = 0f
            shadowOffset = CGSizeMake(0.0, 0.0)
            shadowRadius = 0.0
        }

        if(theme.theme.blurBackground.value == 0.0) effectBackground?.let {
            it.removeFromSuperview()
            effectBackground = null
        } else {
            val effect = (effectBackground ?: BlurBackgroundView().apply {
                addChildTarget.insertSubview(this, 0)
                effectBackground = this
            })
            effect.effect = when(theme.theme.blurBackground.value) {
                in 0.0..<5.0 -> UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleExtraLight)
                in 5.0..<10.0 -> UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleLight)
                else -> UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular)
            }
        }

        native.extensionPadding = paddingByEdge ?: when {
            !theme.padding -> null
            else -> theme.theme.padding
        }

        val fullyApply = theme.drawBackground
        native.clipsToBounds = fullyApply
        animateIfAllowed {
//            native.clearOldLayers()
//            if(fullyApply) applyThemeBackground(theme, native, parent?.mySpacing ?: theme.gap)
//            native.layoutLayers()
            if (!fullyApply) {
                backgroundLayer?.removeFromSuperlayer()
                backgroundLayer = null
                return@animateIfAllowed
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
                    if (!disableBackground) {
                        when (val b = theme.theme.background) {
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
                                    val i = Color.interpolate(
                                        b.base,
                                        b.alternate,
                                        (sin(clockMillis() / 2000.0 * PI * 2) / 2 + 0.5).toFloat()
                                    ).toUiColor().CGColor!!
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
                    }
                    borderWidth = theme.theme.outlineWidth.value
                    borderColor = theme.theme.outline.closestColor().toUiColor().CGColor
                }

                zPosition = -99999.0
                parentSpacing = (parent?.mySpacingForChildren ?: 0.px).value
                desiredCornerRadius = theme.theme.cornerRadii

                val bounds = this@RView.native.layerSize()

                frame = bounds
                refreshCorners()
            }

            // Apply transformation if present
            theme.theme.transform?.let { transform ->
                // Apply transformations to the native view's layer
                if (transform.translationX != 0.0 || transform.translationY != 0.0 || transform.translationZ != 0.0) {
                    // Apply translation
                    native.layer.transform = platform.QuartzCore.CATransform3DMakeTranslation(
                        transform.translationX,
                        transform.translationY,
                        transform.translationZ
                    )
                } else if (transform.rotation != 0.0) {
                    // Apply rotation (convert degrees to radians)
                    val radians = transform.rotation * (kotlin.math.PI / 180.0)
                    native.layer.transform = platform.QuartzCore.CATransform3DMakeRotation(radians, 0.0, 0.0, 1.0)
                } else if (transform.scaleX != 1.0 || transform.scaleY != 1.0) {
                    // Apply scale
                    native.layer.transform = platform.QuartzCore.CATransform3DMakeScale(
                        transform.scaleX,
                        transform.scaleY,
                        1.0
                    )
                } else {
                    // Default identity transform
                    native.layer.transform = platform.QuartzCore.CATransform3DIdentity.readValue()
                }
            } ?: run {
                // Reset transform if no transformation is specified
                native.layer.transform = platform.QuartzCore.CATransform3DIdentity.readValue()
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

    protected open val addChildTarget: UIView get() = native
    actual override fun internalAddChild(index: Int, view: RView) {
        val existingView = children.getOrNull(index)
        val existingIndex = addChildTarget.subviews.indexOfFirst { it == existingView?.native }
        if (existingIndex == -1)
            addChildTarget.addSubview(view.native)
        else
            addChildTarget.insertSubview(view.native, existingIndex.toLong())
    }

    actual override fun internalRemoveChild(index: Int) {
        if (index >= children.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${addChildTarget.subviews.size}")
        }
        children[index].native.removeFromSuperview()
    }

    actual override fun internalClearChildren() {
        children.toList().forEach {
            (it as UIView).let {
                it.removeFromSuperview()
            }
        }
    }
}

public var animationsEnabled: Boolean = true
public var isInAnimationBlock: Boolean = false
public actual val RView.areAnimationsEnabled: Boolean get() = com.lightningkite.kiteui.views.animationsEnabled
public actual inline fun RView.withoutAnimation(action: () -> Unit) {
    native.withoutAnimation(action)
}

inline fun UIView.debugPrint(get: ()->String) {
    if(debugMode && viewDebugTarget?.native == this)
        Log.tag("viewDebugTarget").info(get())
}
inline fun UIView.withoutAnimation(action: () -> Unit) {
    assertMainThread()
    val before = animationsEnabled
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
        animationsEnabled = before
    }
}

public inline fun UIView.animateIfAllowed(crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(/*extensionAnimationDuration ?:*/ 0.5) {
        val before = isInAnimationBlock
        isInAnimationBlock = true
        try {
            action()
        } finally {
            isInAnimationBlock = before
        }
    } else {
        action()
    }
}

public inline fun RView.animateIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { onComplete() },
        animations = {
            val before = isInAnimationBlock
            isInAnimationBlock = true
            try {
                action()
            } finally {
                isInAnimationBlock = before
            }
        }
    ) else {
        action()
        onComplete()
    }
}

public inline fun RView.transitionIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.transitionWithView(
        view = native,
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { onComplete() },
        animations = {
            val before = isInAnimationBlock
            isInAnimationBlock = true
            try {
                action()
            } finally {
                isInAnimationBlock = before
            }
        },
        options = UIViewAnimationOptionTransitionCrossDissolve
    ) else {
        action()
        onComplete()
    }
}
