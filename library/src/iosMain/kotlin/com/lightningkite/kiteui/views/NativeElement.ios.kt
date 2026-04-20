package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.direct.ScrollView
import com.lightningkite.kiteui.views.direct.WrapperView
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSItemProvider
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CATransaction
import platform.QuartzCore.CATransform3DIdentity
import platform.QuartzCore.CATransform3DMakeRotation
import platform.QuartzCore.CATransform3DMakeScale
import platform.QuartzCore.CATransform3DMakeTranslation
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import com.lightningkite.kiteui.models.LiveRegionMode
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.accessibilityHint
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityHint
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setAccessibilityTraits
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIDragInteraction
import platform.UIKit.UIDragInteractionDelegateProtocol
import platform.UIKit.UIDragItem
import platform.UIKit.UIDragSessionProtocol
import platform.UIKit.UIDropInteraction
import platform.UIKit.UIDropInteractionDelegateProtocol
import platform.UIKit.UIDropProposal
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionTransitionCrossDissolve
import platform.UIKit.UIVisualEffectView
import platform.UIKit.addInteraction
import platform.UIKit.removeInteraction
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.native.ref.WeakReference
import kotlin.time.DurationUnit

actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {
    abstract val native: UIView
    protected open val addChildTarget: UIView get() = native

    var tag: Any? = null

    // --- ACCESSIBILITY ---

    override var accessibleLabel: String?
        get() = super.accessibleLabel
        set(value) {
            super.accessibleLabel = value
            native.accessibilityLabel = value
        }

    override var accessibleSemantic: AccessibleSemantic?
        get() = super.accessibleSemantic
        set(value) {
            super.accessibleSemantic = value
            when (value) {
                is AccessibleSemantic.Heading -> {
                    native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitHeader
                }
                else -> {
                    // Clear header trait if it was previously set
                    native.accessibilityTraits = native.accessibilityTraits and UIAccessibilityTraitHeader.inv()
                }
            }
        }

    override var accessibleLiveRegion: LiveRegionMode
        get() = super.accessibleLiveRegion
        set(value) {
            super.accessibleLiveRegion = value
            // iOS does not have a direct equivalent to Android's accessibilityLiveRegion.
            // We store the mode and post UIAccessibility notifications from content-change sites.
        }

    override var labelFor: Element?
        get() = super.labelFor
        set(value) {
            super.labelFor = value
            if (value != null) {
                val targetElement = value.underlyingNativeElement as NativeElement
                if (targetElement.accessibleLabel == null) {
                    val text = (outermostElement as? com.lightningkite.kiteui.views.direct.TextView)?.content
                    if (text != null) {
                        targetElement.native.accessibilityLabel = text
                    }
                }
            }
        }

    override var describedBy: Element?
        get() = super.describedBy
        set(value) {
            super.describedBy = value
            if (value != null) {
                val text = (value.underlyingNativeElement as? com.lightningkite.kiteui.views.direct.TextView)?.content
                if (text != null && text.isNotBlank()) {
                    native.accessibilityHint = text
                }
            } else {
                native.accessibilityHint = null
            }
        }

    /** Posts a screen-changed notification if this element is marked as a live region. */
    fun postLiveRegionNotificationIfNeeded() {
        if (accessibleLiveRegion != LiveRegionMode.None) {
            UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, native)
        }
    }

    actual override var showOnPrint: Boolean = true

    var sizeConstraints: SizeConstraints?
        get() = native.extensionSizeConstraints
        set(value) {
            native.extensionSizeConstraints = value
        }

    actual override var opacity: Double = 1.0
        set(value) {
            field = value
            animateIfAllowed {
                native.alpha = value
            }
        }

    actual override var shown: Boolean = true
        set(value) {
            field = value
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

    actual override var visible: Boolean = true
        set(value) {
            field = value
            animateIfAllowed {
                native.alpha = if (value) 1.0 else 0.0
            }
        }

    actual override var ignoreInteraction: Boolean = false
        set(value) {
            field = value
            native.extensionIgnoreInteraction = value
        }

    // Update padding based on safe insets
    actual override fun refreshPadding() {
        val value = appliedPadding
        native.extensionPadding = value
        native.informParentOfSizeChange()
    }

    actual fun screenRectangle(): Rect? {
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

    actual fun parentRectangle(): Rect? {
        return native.frame.useContents {
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


    protected var previousLoadAnimationHandle: (() -> Unit)? = null
    protected var backgroundLayer: CAGradientLayerResizing? = null

    /**
     * No matter how we set the zPosition or the "at" argument of the insertSublayer call, layers always cover the
     * content of UIImageView elements. Thus, we require a method of disabling the KiteUI background drawing entirely
     * for subclasses of Element. In this way, themes with a back may be applied so that corner radius is respected
     * without drawing anything that would cover the content of the view.
     */
    protected open val disableBackground = false

    class BlurBackgroundView : UIVisualEffectView(UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular))

    var effectBackground: BlurBackgroundView? = null

    actual override fun nativeApplyTheme(theme: ThemeAndBack) {
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

        if (theme.theme.blurBackground.value == 0.0) effectBackground?.let {
            it.removeFromSuperview()
            effectBackground = null
        } else {
            val effect = (effectBackground ?: BlurBackgroundView().apply {
                addChildTarget.insertSubview(this, 0)
                effectBackground = this
            })
            effect.effect = when (theme.theme.blurBackground.value) {
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
                parentSpacing = (parent?.spacingForChildCornerRadii ?: 0.px).value
                desiredCornerRadius = theme.theme.cornerRadii
                desiredCornerShape = theme.theme.cornerShape

                val bounds = this@NativeElement.native.layerSize()

                frame = bounds
                refreshCorners()
            }

            // Apply transformation if present
            theme.theme.transform?.let { transform ->
                // Apply transformations to the native view's layer
                if (transform.translationX != 0.0 || transform.translationY != 0.0 || transform.translationZ != 0.0) {
                    // Apply translation
                    native.layer.transform = CATransform3DMakeTranslation(
                        transform.translationX,
                        transform.translationY,
                        transform.translationZ
                    )
                } else if (transform.rotation != 0.0) {
                    // Apply rotation (convert degrees to radians)
                    val radians = transform.rotation * (PI / 180.0)
                    native.layer.transform = CATransform3DMakeRotation(radians, 0.0, 0.0, 1.0)
                } else if (transform.scaleX != 1.0 || transform.scaleY != 1.0) {
                    // Apply scale
                    native.layer.transform = CATransform3DMakeScale(
                        transform.scaleX,
                        transform.scaleY,
                        1.0
                    )
                } else {
                    // Default identity transform
                    native.layer.transform = CATransform3DIdentity.readValue()
                }
            } ?: run {
                // Reset transform if no transformation is specified
                native.layer.transform = CATransform3DIdentity.readValue()
            }
        }
    }

    @OptIn(ExperimentalNativeApi::class)
    override fun leakDetect() {
        super.leakDetect()
        WeakReference(native).checkLeakAfterDelay(1_000)
    }

    @OptIn(OverrideOnly::class)
    override fun onStartup() {
        super.onStartup()
        ObjCountTrackers.track(this)
        ObjCountTrackers.track(native)
    }

    private var dragInteraction: UIDragInteraction? = null
    private var dragDelegate: DragInteractionDelegate? = null

    actual override var dragData: DragData? = null
        set(value) {
            field = value
            if (value != null) {
                if (dragInteraction == null) {
                    val interaction = UIDragInteraction(DragInteractionDelegate(this).also {
                        dragDelegate = it
                    })
                    interaction.enabled = true
                    native.addInteraction(interaction)
                    native.setUserInteractionEnabled(true)
                    native.userInteractionEnabled = true
                    this.dragInteraction = interaction
                }
            } else {
                // Remove the interaction
                dragInteraction?.let { native.removeInteraction(it) }
                dragInteraction = null
                dragDelegate = null
            }
        }

    private class DragInteractionDelegate(view: Element) : NSObject(), UIDragInteractionDelegateProtocol {
        @OptIn(ExperimentalNativeApi::class)
        private val owner = WeakReference(view)

        @ObjCSignatureOverride
        @OptIn(ExperimentalNativeApi::class)
        override fun dragInteraction(interaction: UIDragInteraction, itemsForBeginningSession: UIDragSessionProtocol): List<UIDragItem> {
            val view = owner.get() ?: return listOf<UIDragItem>()
            val data = view.dragData ?: return listOf<UIDragItem>()
            view.parent?.let {
                addChildDropInteractionToParentScrollViews(it)
            }

            val itemProvider = NSItemProvider(item = data.data as? NSString, typeIdentifier = data.mimeType)
            val dragItem = UIDragItem(itemProvider)
            dragItem.localObject = data
            return listOf(dragItem)
        }


        fun addChildDropInteractionToParentScrollViews(currentView: ContainerElement) {
            currentView.children.forEach { child ->
                // Check if this child is a ScrollView
                if (child is ScrollView) {
                    child.children.forEach {
                        if (it.underlyingNativeElement.dropInteractionDelegate != null && child.scrollViewDropInteraction == null) {
                            child.dropInteractionDelegate = it.underlyingNativeElement.dropInteractionDelegate
                            val interaction = UIDropInteraction(DropInteractionDelegate(child))
                            child.scrollViewDropInteraction = interaction
                            child.native.addInteraction(interaction)
                        }
                    }
                }
                if (child is ContainerElement && child.children.isNotEmpty()) {
                    addChildDropInteractionToParentScrollViews(child)
                }
            }
        }

        fun removeChildDropInteractionToParentScrollViews(currentView: ContainerElement) {
            currentView.children.forEach { child ->
                // Check if this child is a ScrollView
                if (child is ScrollView) {
                    child.scrollViewDropInteraction?.let { interaction ->
                        child.native.removeInteraction(interaction)
                        child.scrollViewDropInteraction = null
                    }
                    child.dropInteractionDelegate = null
                }
                if (child is ContainerElement && child.children.isNotEmpty()) {
                    removeChildDropInteractionToParentScrollViews(child)
                }
            }
        }

        @ObjCSignatureOverride
        @OptIn(ExperimentalNativeApi::class)
        override fun dragInteraction(interaction: platform.UIKit.UIDragInteraction, sessionWillBegin: platform.UIKit.UIDragSessionProtocol) {
            val view = owner.get() ?: return
            view.parent?.let {
                addChildDropInteractionToParentScrollViews(it)
            }
        }

        @ObjCSignatureOverride
        @OptIn(ExperimentalNativeApi::class)
        override fun dragInteraction(
            interaction: platform.UIKit.UIDragInteraction,
            session: platform.UIKit.UIDragSessionProtocol,
            didEndWithOperation: kotlin.ULong /* from: platform.UIKit.UIDropOperation */
        ): kotlin.Unit {
            val view = owner.get() ?: return
            view.parent?.let {
                removeChildDropInteractionToParentScrollViews(it)
            }
        }
    }


    var dropInteraction: UIDropInteraction? = null
    var dropInteractionDelegate: DropInteractionDelegate? = null
    var scrollViewDropInteraction: UIDropInteraction? = null

    actual override var dropTargetDelegate: DropTargetDelegate? = null
        set(value) {
            field = value
            if (value != null) {
                if (dropInteraction == null) {
                    val interaction = UIDropInteraction(DropInteractionDelegate(this).also {
                        dropInteractionDelegate = it
                    })
                    native.userInteractionEnabled = true
                    native.addInteraction(interaction)
                    this.dropInteraction = interaction
                }
            } else {
                dropInteraction?.let { native.removeInteraction(it) }
                dropInteraction = null
                dropInteractionDelegate = null
            }
        }

    // A private delegate class to handle drop events
    class DropInteractionDelegate(view: Element) : NSObject(), UIDropInteractionDelegateProtocol {
        @OptIn(ExperimentalNativeApi::class)
        private val owner = WeakReference(view)

        private fun getDragDataPlaceholder(session: UIDropSessionProtocol): DragData? {
            val local = session.localDragSession?.localContext as? DragData
            if (local != null) return local

            val provider = (session.items.firstOrNull() as? UIDragItem)?.itemProvider ?: return null
            val mimeType = provider.registeredTypeIdentifiers.firstOrNull() as? String ?: "text/plain"

            return DragData(mimeType = mimeType, data = "", label = "External Data", dragShadow = null)
        }


        @OptIn(ExperimentalNativeApi::class)
        @ObjCSignatureOverride
        override fun dropInteraction(interaction: UIDropInteraction, canHandleSession: UIDropSessionProtocol): Boolean {
            val view = owner.get() ?: return false
            return true
        }

        @OptIn(ExperimentalNativeApi::class)
        @ObjCSignatureOverride
        override fun dropInteraction(interaction: UIDropInteraction, sessionDidUpdate: UIDropSessionProtocol): UIDropProposal {
            val view = owner.get() ?: return platform.UIKit.UIDropProposal(platform.UIKit.UIDropOperationCancel)
            val delegate = view.dropTargetDelegate ?: (view as? ContainerElement)?.children?.firstOrNull { it.dropTargetDelegate != null }?.dropTargetDelegate
            ?: return platform.UIKit.UIDropProposal(platform.UIKit.UIDropOperationCancel)
            getDragDataPlaceholder(sessionDidUpdate)?.let { data ->
                val targetView = if (view is ScrollView) view.children.firstOrNull { it.dropTargetDelegate != null } ?: view else view
                val location = sessionDidUpdate.locationInView(targetView.native)
                val event = DragEvent(data, location.useContents { x }, location.useContents { y })
                delegate.over(event)
            }

            return platform.UIKit.UIDropProposal(platform.UIKit.UIDropOperationMove)
        }


        @ObjCSignatureOverride
        @OptIn(ExperimentalNativeApi::class)
        override fun dropInteraction(interaction: UIDropInteraction, performDrop: UIDropSessionProtocol) {
            val view = owner.get() ?: return
            val delegate = view.dropTargetDelegate ?: (view as? ContainerElement)?.children?.firstOrNull { it.dropTargetDelegate != null }?.dropTargetDelegate
            ?: return
            val localData = (performDrop.items.firstOrNull() as? UIDragItem)?.localObject as? DragData
            if (localData != null) {
                // Use consistent location calculation - target the view with the delegate
                val targetView = if (view is ScrollView) view.children.firstOrNull { it.dropTargetDelegate != null } ?: view else view
                val location = performDrop.locationInView(targetView.native)
                val event = DragEvent(
                    data = localData,
                    xInView = location.useContents { x },
                    yInView = location.useContents { y },
                )
                delegate.drop(event)
                return
            }
        }
    }
}

val Element.native: UIView get() = underlyingNativeElement.native


