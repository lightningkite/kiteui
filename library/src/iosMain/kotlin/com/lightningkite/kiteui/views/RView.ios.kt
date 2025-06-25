package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.ImageCache
import com.lightningkite.kiteui.views.direct.WrapperView
import com.lightningkite.kiteui.views.direct.inBackground
import com.lightningkite.kiteui.views.direct.load
import com.lightningkite.kiteui.views.direct.render
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.NativePlacement
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.invoke
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.nativeHeap.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGColorCreateWithPattern
import platform.CoreGraphics.CGColorRelease
import platform.CoreGraphics.CGColorSpaceCreatePattern
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGPatternCallbacks
import platform.CoreGraphics.CGPatternCreate
import platform.CoreGraphics.CGPatternDrawPatternCallback
import platform.CoreGraphics.CGPatternRelease
import platform.CoreGraphics.CGPatternTiling
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.QuartzCore.kCAGravityResize
import platform.QuartzCore.kCAGravityResizeAspectFill
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionTransitionCrossDissolve
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.PI
import kotlin.math.max
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
    protected var subBackgroundLayer: CALayerResizing? = null
    protected var subBackgroundLayerLoadedImage: ImagePaint? = null

    /**
     * No matter how we set the zPosition or the "at" argument of the insertSublayer call, layers always cover the
     * content of UIImageView elements. Thus, we require a method of disabling the KiteUI background drawing entirely
     * for subclasses of RView. In this way, themes with a back may be applied so that corner radius is respected
     * without drawing anything that would cover the content of the view.
     */
    protected open val disableBackground = false

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

        native.extensionPadding = paddingByEdge ?: when {
            !theme.padding -> null
            else -> theme.theme.padding
        }

        val fullyApply = theme.drawBackground
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
            var shouldRemoveSublayer = true
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

                            is ImagePaint -> {
                                if(subBackgroundLayerLoadedImage == b) return@with
                                when(b.mode) {
                                    ImagePaintMode.Crop -> {
                                        // Set initial background to overlay color
                                        val c = b.overlayColor.toUiColor().CGColor!!
                                        this.type = kCAGradientLayerAxial
                                        this.locations = listOf(NSNumber.numberWithFloat(0f), NSNumber.numberWithFloat(1f))
                                        this.colors = listOf(c, c).map { it.toObjcId() }
                                        this.startPoint = CGPointMake(0.0, 0.0)
                                        this.endPoint = CGPointMake(1.0, 1.0)

                                        // Create a new CALayer for the image
                                        val imageLayer = subBackgroundLayer ?: CALayerResizing().also {
                                            it.frame = this.bounds
                                            it.zPosition = -99999.0
                                            it.contents = null
                                            it.backgroundColor = null
                                            it.setNeedsDisplay()
                                            subBackgroundLayer = it
                                            native.layer.insertSublayer(it, 0U)
                                        }
                                        subBackgroundLayerLoadedImage = b
                                        imageLayer.contentsGravity = kCAGravityResizeAspectFill
                                        shouldRemoveSublayer = false

                                        // Load the image based on source type
                                        // TODO: use a scope limited to this theme application
                                        launch {
                                            imageLayer.contents = b.source.load(null)?.CGImage.also {
                                                println("Loaded image $it for layer")
                                            }
                                            imageLayer.setNeedsDisplay()
                                        }
                                    }

                                    ImagePaintMode.Repeating -> {
                                        // Set initial background to overlay color
                                        val c = b.overlayColor.toUiColor().CGColor!!
                                        this.type = kCAGradientLayerAxial
                                        this.locations = listOf(NSNumber.numberWithFloat(0f), NSNumber.numberWithFloat(1f))
                                        this.colors = listOf(c, c).map { it.toObjcId() }
                                        this.startPoint = CGPointMake(0.0, 0.0)
                                        this.endPoint = CGPointMake(1.0, 1.0)

                                        // Create a new CALayer for the image
                                        val imageLayer = subBackgroundLayer ?: CALayerResizing().also {
                                            it.frame = this.bounds
                                            it.zPosition = -99999.0
                                            it.contents = null
                                            it.backgroundColor = null
                                            it.setNeedsDisplay()
                                            subBackgroundLayer = it
                                            native.layer.insertSublayer(it, 0U)
                                        }
                                        subBackgroundLayerLoadedImage = b
                                        shouldRemoveSublayer = false

                                        // Load the image based on source type
                                        // TODO: use a scope limited to this theme application
                                        launch {
                                            b.source.load(null)?.CGImage?.let {
                                                val space = CGColorSpaceCreatePattern(null)
                                                val width = CGImageGetWidth(it).toDouble()
                                                val height = CGImageGetHeight(it).toDouble()
                                                val pattern = CGPatternCreate(
                                                    info = it,
                                                    bounds = CGRectMake(0.0, 0.0, width, height),
                                                    matrix = CGAffineTransformIdentity.readValue(),
                                                    xStep = width,
                                                    yStep = height,
                                                    tiling = CGPatternTiling.kCGPatternTilingConstantSpacing,
                                                    isColored = true,
                                                    callbacks = patternCallbacks.ptr
                                                )
                                                val color = CGColorCreateWithPattern(space, pattern, cValuesOf(1.0))
                                                imageLayer.backgroundColor = color
                                                CGColorSpaceRelease(space)
                                                CGPatternRelease(pattern)
                                                imageLayer.setNeedsDisplay()
                                                CGColorRelease(color)
                                            }
                                        }
                                    }
                                }
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

                zPosition = -99998.0
                parentSpacing = (parent?.mySpacingForChildren ?: 0.px).value
                desiredCornerRadius = theme.theme.cornerRadii

                if(shouldRemoveSublayer) {
                    subBackgroundLayer?.removeFromSuperlayer()
                    subBackgroundLayer = null
                    subBackgroundLayerLoadedImage = null
                }

                val bounds = this@RView.native.layerSize()

                frame = bounds
                subBackgroundLayer?.frame = bounds
                subBackgroundLayer?.refreshCorners()
                refreshCorners()
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
        if (index == addChildTarget.subviews.size)
            addChildTarget.addSubview(view.native)
        else
            addChildTarget.insertSubview(view.native, index.toLong())
        if (children[index].native != addChildTarget.subviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${addChildTarget.subviews}")
    }

    actual override fun internalRemoveChild(index: Int) {
        if (children[index].native != addChildTarget.subviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${addChildTarget.subviews}")
        if (index >= addChildTarget.subviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${addChildTarget.subviews.size}")
        }
        (addChildTarget.subviews[index] as UIView).removeFromSuperview()
    }

    actual override fun internalClearChildren() {
        addChildTarget.subviews.toList().forEach {
            (it as UIView).let {
                it.removeFromSuperview()
            }
        }
    }
}

private val patternCallbacks = nativeHeap.alloc<CGPatternCallbacks> {
    version = 0U
    drawPattern = staticCFunction { pointer, context ->
        pointer as CGImageRef
        CGContextDrawImage(context, CGRectMake(0.0, 0.0, CGImageGetWidth(pointer).toDouble(), CGImageGetHeight(pointer).toDouble()), pointer)
    }
    releaseInfo = staticCFunction { pointer ->
        CGImageRelease(pointer as CGImageRef)
    }
}

var animationsEnabled: Boolean = true
var isInAnimationBlock: Boolean = false
actual val RView.areAnimationsEnabled: Boolean get() = animationsEnabled
actual inline fun RView.withoutAnimation(action: () -> Unit) {
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

inline fun UIView.animateIfAllowed(crossinline action: () -> Unit) {
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

inline fun RView.animateIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
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

inline fun RView.transitionIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
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
