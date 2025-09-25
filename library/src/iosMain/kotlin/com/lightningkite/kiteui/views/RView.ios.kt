package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.RawImageViewLike
import com.lightningkite.kiteui.views.direct.WrapperView
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIDragItem
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIVibrancyEffect
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionTransitionCrossDissolve
import platform.UIKit.UIVisualEffectView
import platform.UIKit.removeInteraction
import platform.darwin.NSObject
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
        native.clipsToBounds = theme.drawBackground
        applyBackgroundChanges(theme)
    }

    protected fun applyBackgroundChanges(theme: ThemeAndBack) {
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
            it.native.removeFromSuperview()
        }
    }

    // Add this property inside your RView class
    private var dragInteraction: UIDragInteraction? = null

    // Replace your existing dragData property with this
    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            if (value != null) {
                if (dragInteraction == null) {
                    // Create and add the interaction
                    val interaction = UIDragInteraction(DragInteractionDelegate(this))
                    native.addInteraction(interaction)
                    native.userInteractionEnabled = true // Drags require user interaction
                    this.dragInteraction = interaction
                }
            } else {
                // Remove the interaction
                dragInteraction?.let { native.removeInteraction(it) }
                dragInteraction = null
            }
        }

    // A private delegate class to handle drag events
    private class DragInteractionDelegate(view: RView) : NSObject(), UIDragInteractionDelegateProtocol {
        private val owner = WeakReference(view)

        override fun dragInteraction(
            interaction: UIDragInteraction,
            itemsForBeginningSession: platform.UIKit.UIDragSession
        ): List<*> {
            val view = owner.get() ?: return listOf<UIDragItem>()
            val data = view.dragData ?: return listOf<UIDragItem>()

            // Convert your common DragData to an NSItemProvider
            val itemProvider = NSItemProvider(item = data.data as? NSString, typeIdentifier = data.mimeType)
            val dragItem = UIDragItem(itemProvider)

            // Store the original DragData in the localContext for in-app drops
            dragItem.localObject = data
            return listOf(dragItem)
        }
    }


    private var dropInteraction: UIDropInteraction? = null

    // Replace your existing dropTargetDelegate property with this
    override var dropTargetDelegate: DropTargetDelegate?
        get() = super.dropTargetDelegate
        set(value) {
            super.dropTargetDelegate = value
            if (value != null) {
                if (dropInteraction == null) {
                    // Create and add the interaction
                    val interaction = UIDropInteraction(DropInteractionDelegate(this))
                    native.addInteraction(interaction)
                    this.dropInteraction = interaction
                }
            } else {
                // Remove the interaction
                dropInteraction?.let { native.removeInteraction(it) }
                dropInteraction = null
            }
        }

    // A private delegate class to handle drop events
    private class DropInteractionDelegate(view: RView) : NSObject(), UIDropInteractionDelegateProtocol {
        private val owner = WeakReference(view)

        private fun getDragData(session: platform.UIKit.UIDropSession): DragData? {
            // Prioritize local object if available (for in-app drags)
            val local = session.localDragSession?.localContext as? DragData
            if (local != null) return local

            // Fallback for external drags - this part might need more robust handling
            val provider = session.items.firstOrNull()?.itemProvider ?: return null
            // This is a simplified example; real implementation may require async loading
            val mimeType = provider.registeredTypeIdentifiers.firstOrNull() as? String ?: "text/plain"
            val data = provider.loadObjectOfClass(NSString) { str, _ ->
                // This is async, a full implementation would use a completion handler
            }
            return DragData(mimeType = mimeType, data = "External Data")
        }

        override fun dropInteraction(
            interaction: UIDropInteraction,
            canHandleSession: platform.UIKit.UIDropSession
        ): Boolean {
            // Allow handling of sessions that have our supported data types
            return owner.get()?.dropTargetDelegate != null
        }

        override fun dropInteraction(
            interaction: UIDropInteraction,
            sessionDidUpdate: platform.UIKit.UIDropSession
        ): UIDropProposal {
            // Called when the drag cursor enters or moves within the view
            val view = owner.get() ?: return UIDropProposal(kUIDropOperationCancel)
            val delegate = view.dropTargetDelegate ?: return UIDropProposal(kUIDropOperationCancel)
            val location = sessionDidUpdate.locationInView(view.native)
            val data = getDragData(sessionDidUpdate) ?: return UIDropProposal(kUIDropOperationCancel)

            val event = DragEvent(
                data = data,
                xInView = location.useContents { x },
                yInView = location.useContents { y },
            )
            delegate.over(event)

            return UIDropProposal(kUIDropOperationCopy)
        }

        override fun dropInteraction(interaction: UIDropInteraction, sessionDidEnter: platform.UIKit.UIDropSession) {
            val view = owner.get() ?: return
            val delegate = view.dropTargetDelegate ?: return
            val location = sessionDidEnter.locationInView(view.native)
            val data = getDragData(sessionDidEnter) ?: return

            val event = DragEvent(
                data = data,
                xInView = location.useContents { x },
                yInView = location.useContents { y },
            )
            delegate.enter(event)
        }

        override fun dropInteraction(interaction: UIDropInteraction, sessionDidExit: platform.UIKit.UIDropSession) {
            val view = owner.get() ?: return
            val delegate = view.dropTargetDelegate ?: return
            val location = sessionDidExit.locationInView(view.native)
            val data = getDragData(sessionDidExit) ?: return

            val event = DragEvent(
                data = data,
                xInView = location.useContents { x },
                yInView = location.useContents { y },
            )
            delegate.exit(event)
        }

        override fun dropInteraction(interaction: UIDropInteraction, performDrop: platform.UIKit.UIDropSession) {
            val view = owner.get() ?: return
            val delegate = view.dropTargetDelegate ?: return
            val location = performDrop.locationInView(view.native)
            val data = getDragData(performDrop) ?: return

            val event = DragEvent(
                data = data,
                xInView = location.useContents { x },
                yInView = location.useContents { y },
            )
            delegate.drop(event)
        }
    }


}

var animationsEnabled: Boolean = true
var isInAnimationBlock: Boolean = false
actual val RView.areAnimationsEnabled: Boolean get() = com.lightningkite.kiteui.views.animationsEnabled
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


