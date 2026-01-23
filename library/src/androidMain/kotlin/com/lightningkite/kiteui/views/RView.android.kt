package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.res.ColorStateList
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.direct.CoordinatorFrame
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.colorInt
import com.lightningkite.reactive.context.*
import kotlin.math.min
import kotlin.math.roundToInt

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: View

    var removeListener: (() -> Unit)? = null
    init {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw Exception("Cannot create views on any thread but the main thread")

        onRemove {
            removeListener?.invoke()
        }
    }

    actual override var showOnPrint: Boolean = true

    open fun defaultLayoutParams(): LayoutParams =
        FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    override var opacity: Double
        get() = super.opacity
        set(value) {
            super.opacity = value
            if (animationsEnabled) {
                ValueAnimator.ofFloat(native.alpha, value.toFloat()).apply {
                    duration = theme.transitionDuration.inWholeMilliseconds
                    addUpdateListener {
                        native.alpha = animatedValue as Float
                    }
                }.start()
            } else {
                native.alpha = value.toFloat()
            }
        }

    override var shown: Boolean
        get() = super.shown
        set(value) {
            super.shown = value
            // Setting visibility to GONE does not work if an animation is running
            if (!shown) {
                native.clearAnimation()
            }
            native.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            (parent?.native as? DesiredSizeView)?.apply {
                visibility = native.visibility
            }
        }

    override var visible: Boolean
        get() = super.visible
        set(value) {
            super.visible = value
            native.visibility = if (value) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            for (child in children) child.updateCorners()
        }

    override var ignoreInteraction: Boolean
        get() = super.ignoreInteraction
        set(value) {
            super.ignoreInteraction = value
            native.isClickable = !value
            native.isFocusable = !value
        }

    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            for (child in children) child.updateCorners()
        }

    override var transitionId: String?
        get() = super.transitionId
        set(value) {
            super.transitionId = value
            native.transitionName = value
        }

    private class DragShadowBuilder(val shadow: DragShadow) : View.DragShadowBuilder(shadow.view.native) {
        override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
            val view = shadow.view.native
            // Android cant have touch point below zero clamp to 0 or larger
            outShadowSize?.set(view.width.coerceAtLeast(0), view.height.coerceAtLeast(0))
            outShadowTouchPoint?.set(
                (when (shadow.xAlign) {
                    Align.Start -> 0
                    Align.Center, Align.Stretch -> view.width / 2
                    Align.End -> view.width
                } + (shadow.xOffset?.px?.roundToInt() ?: 0)).coerceAtLeast(0),
                (when (shadow.yAlign) {
                    Align.Start -> 0
                    Align.Center, Align.Stretch -> view.height / 2
                    Align.End -> view.height
                } + (shadow.yOffset?.px?.roundToInt() ?: 0)).coerceAtLeast(0)
            )
        }
    }

    // drag 'n drop
    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            if (value == null) native.setOnLongClickListener(null)
            else native.setOnLongClickListener {
                native.startDrag(
                    ClipData(value.label, arrayOf(value.mimeType), ClipData.Item(value.data)),
                    value.dragShadow?.let(::DragShadowBuilder) ?: View.DragShadowBuilder(native),
                    value,
                    0
                )
                true
            }
        }
    override var dropTargetDelegate: DropTargetDelegate?
        get() = super.dropTargetDelegate
        set(value) {
            super.dropTargetDelegate = value
            if (value == null) native.setOnDragListener(null)
            else native.setOnDragListener { v, event ->

                val ev =
                    DragEvent(
                        data = event.clipData?.let {
                            DragData(
                                it.description.label.toString(),
                                (0..<it.itemCount).associate { i -> it.description.getMimeType(i) to it.getItemAt(i).text.toString() },
                            )
                        } ?: (event.localState as? DragData) ?: throw IllegalStateException("ClipData was empty for view $v"),
                        xInView = event.x.toDouble(),
                        yInView = event.y.toDouble(),
                    )
                when (event.action) {
                    android.view.DragEvent.ACTION_DRAG_LOCATION -> value.over(ev)

                    android.view.DragEvent.ACTION_DRAG_ENTERED -> value.enter(ev)
                    android.view.DragEvent.ACTION_DRAG_EXITED -> value.exit(ev)

                    android.view.DragEvent.ACTION_DRAG_ENDED -> value.end(ev)

                    android.view.DragEvent.ACTION_DROP -> value.drop(ev)
                    else -> true
                }
            }
        }


    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        generateSequence(native) {
            it.parent as? View
        }.firstOrNull() {
            when (it) {
                is HorizontalScrollView -> {
                    scrollToView(it, native, animate)
                    true
                }

                is ScrollView -> {
                    scrollToView(it, native, animate)
                    true
                }

                is NestedScrollView -> {
                    scrollToView(it, native, animate)
                    true
                }

                else -> false
            }
        }
    }

    actual override fun requestFocus() {
        afterTimeout(16) {
            native.requestFocus()
        }
    }

    actual override fun screenRectangle(): Rect? {
        val r = android.graphics.Rect()
        native.getGlobalVisibleRect(r)
        return Rect(
            left = r.left.toDouble(),
            top = r.top.toDouble(),
            right = r.right.toDouble(),
            bottom = r.bottom.toDouble(),
        )
    }

    protected var background: Drawable? = null
        set(value) {
            field = value
            native.background = value
        }
    protected var backgroundBlock: GradientDrawable? = null

    protected fun updateCorners() {
        val cr = when (val it = theme.cornerRadii) {
            is CornerRadii.AdaptiveToSpacing -> min((parent?.mySpacingForChildren ?: 0.px).value, it.value.value)
            is CornerRadii.Fixed -> it.value.value
            is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(native.width, native.height)
            is CornerRadii.RatioOfSpacing -> it.value * (parent?.mySpacingForChildren ?: 0.px).value
            is CornerRadii.PerCorner -> it.value.value
        }

        val asPerCorner = theme.cornerRadii as? CornerRadii.PerCorner
        val topLeft = if (asPerCorner?.topLeft != false) cr else 0f
        val topRight = if (asPerCorner?.topRight != false) cr else 0f
        val bottomRight = if (asPerCorner?.bottomRight != false) cr else 0f
        val bottomLeft = if (asPerCorner?.bottomLeft != false) cr else 0f

        val radii = floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)

        backgroundBlock?.cornerRadii = radii

        // Also update NeumorphicDrawable if present
        (background as? NeumorphicDrawable)?.setCornerRadii(radii)
    }

    override fun refreshPadding() {
        super.refreshPadding()
        val value = appliedPadding
        native.setPadding(
            value.left.value.toInt(),
            value.top.value.toInt(),
            value.right.value.toInt(),
            value.bottom.value.toInt(),
        )
    }

    // Map to track active animators for each view property
    companion object {
        private val activeAnimators = mutableMapOf<String, ValueAnimator>()
    }

    private fun animateProperty(targetValue: Float, existingAnimator: ValueAnimator?, getter: ()->Float, setter: (Float)->Unit): ValueAnimator? {
        existingAnimator?.cancel()
        if(getter() == targetValue) return null
        
        if (animationsEnabled) {
            return ValueAnimator.ofFloat(getter(), targetValue).apply {
                duration = theme.transitionDuration.inWholeMilliseconds
                addUpdateListener {
                    setter(it.animatedValue as Float)
                }
                start()
            }
        } else {
            setter(targetValue)
            return null
        }
    }
    private var animatorTranslationX: ValueAnimator? = null
    private var animatorTranslationY: ValueAnimator? = null
    private var animatorTranslationZ: ValueAnimator? = null
    private var animatorRotationX: ValueAnimator? = null
    private var animatorRotationY: ValueAnimator? = null
    private var animatorRotation: ValueAnimator? = null
    private var animatorScaleX: ValueAnimator? = null
    private var animatorScaleY: ValueAnimator? = null

    actual override fun applyTheme(theme: ThemeAndBack) {
        val shadows = theme.theme.shadows

        if (theme.drawBackground && shadows != null && shadows.isNotEmpty()) {
            // Use neumorphic/multi-shadow rendering
            native.elevation = 0f // Disable native elevation

            val neumorphicBg = background as? NeumorphicDrawable
            if (neumorphicBg != null) {
                // Update existing neumorphic drawable
                neumorphicBg.setShadows(shadows)
                neumorphicBg.setBackgroundColor(theme.theme.background.colorInt())
                updateCorners()
            } else {
                // Create new neumorphic drawable
                val newBg = NeumorphicDrawable(
                    shadows = shadows,
                    cornerRadius = 0f, // Will be set by updateCorners()
                    backgroundColor = theme.theme.background.colorInt()
                )
                backgroundBlock = null
                background = newBg
                updateCorners()

                // Enable software layer if needed for blur effects
                if (newBg.needsSoftwareLayer()) {
                    native.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                }
            }
        } else {
            // Standard elevation-based shadow rendering
            if (theme.drawBackground) {
                native.elevation = theme.theme.elevation.value
            } else {
                native.elevation = 0f
            }

            // Reset layer type if we were using software rendering
            if (background is NeumorphicDrawable) {
                native.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            }

            if (theme.drawBackground) {
                val backgroundDrawable =
                    theme.theme.backgroundDrawableWithoutCorners(background as? GradientDrawable).also {
                        removeListener?.invoke()
                        removeListener = it.applyGradientRadiusListener(native)
                    }
                backgroundBlock = backgroundDrawable
                updateCorners()
                background = backgroundDrawable
            } else {
                backgroundBlock = null
                background = null
            }
        }
        updateTransform(theme.theme)
    }

    private fun updateTransform(theme: Theme) {
        theme.transform?.let { transform ->
            animatorTranslationX = animateProperty(transform.translationX.toFloat(), animatorTranslationX, { native.translationX }, { native.translationX = it })
            animatorTranslationY = animateProperty(transform.translationY.toFloat(), animatorTranslationY, { native.translationY }, { native.translationY = it })
            animatorTranslationZ = animateProperty(transform.translationZ.toFloat(), animatorTranslationZ, { native.translationZ }, { native.translationZ = it })
            animatorRotationX = animateProperty(transform.rotationX.toFloat(), animatorRotationX, { native.rotationX }, { native.rotationX = it })
            animatorRotationY = animateProperty(transform.rotationY.toFloat(), animatorRotationY, { native.rotationY }, { native.rotationY = it })
            animatorRotation = animateProperty(transform.rotation.toFloat(), animatorRotation, { native.rotation }, { native.rotation = it })
            animatorScaleX = animateProperty(transform.scaleX.toFloat(), animatorScaleX, { native.scaleX }, { native.scaleX = it })
            animatorScaleY = animateProperty(transform.scaleY.toFloat(), animatorScaleY, { native.scaleY }, { native.scaleY = it })
        } ?: run {
            // Reset transformations if no transform is specified
            animatorTranslationX = animateProperty(0f, animatorTranslationX, { native.translationX }, { native.translationX = it })
            animatorTranslationY = animateProperty(0f, animatorTranslationY, { native.translationY }, { native.translationY = it })
            animatorTranslationZ = animateProperty(0f, animatorTranslationZ, { native.translationZ }, { native.translationZ = it })
            animatorRotationX = animateProperty(0f, animatorRotationX, { native.rotationX }, { native.rotationX = it })
            animatorRotationY = animateProperty(0f, animatorRotationY, { native.rotationY }, { native.rotationY = it })
            animatorRotation = animateProperty(0f, animatorRotation, { native.rotation }, { native.rotation = it })
            animatorScaleX = animateProperty(1f, animatorScaleX, { native.scaleX }, { native.scaleX = it })
            animatorScaleY = animateProperty(1f, animatorScaleY, { native.scaleY }, { native.scaleY = it })
        }
    }

    override fun postSetup() {
        super.postSetup()
        // Block touches below
        val wasClickable = native.isClickable
        val wasFocusable = native.isFocusable
        val hasInteractiveParent =
            generateSequence(this) { it.parent }.any { (it.native.isClickable || it.native.isFocusable) && it !is CoordinatorFrame }
//        val previousTrace =
//            generateSequence(this) { it.parent }.map { "  ${it} - ${it.native}, clickable: ${it.native.isClickable}, focusable: ${it.native.isFocusable}" }
//                .toList()
        debugPrint {
            buildString {
                appendLine("--postsetup--")
                appendLine("hasInteractiveParent: $hasInteractiveParent")
                appendLine("interactive parent is: ${generateSequence(this@RView) { it.parent }.find { (it.native.isClickable || it.native.isFocusable) && it !is CoordinatorFrame }}")
                appendLine("wasClickable: $wasClickable")
                appendLine("wasFocusable: $wasFocusable")
                appendLine("ignoreInteraction: $ignoreInteraction")
            }
        }
        if (!hasInteractiveParent && !wasClickable && !wasFocusable && !ignoreInteraction) {
            native.setOnClickListener {
                Log.log("$this ($it) blocked the touch, because hasInteractiveParent = $hasInteractiveParent and wasClickable: ${wasClickable} and wasFocusable: ${wasFocusable}")
            }
        }
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        // Apply parent's default alignment if child doesn't have explicit alignment set
        var needsLayoutParamUpdate = false

        if (view.lastSetHorizontalAlign == Align.Stretch && newChildHorizontalAlign != null) {
            view.lastSetHorizontalAlign = newChildHorizontalAlign!!
            needsLayoutParamUpdate = true
        }
        if (view.lastSetVerticalAlign == Align.Stretch && newChildVerticalAlign != null) {
            view.lastSetVerticalAlign = newChildVerticalAlign!!
            needsLayoutParamUpdate = true
        }

        // If we applied defaults, update layout params (align() modifier wasn't called)
        if (needsLayoutParamUpdate) {
            val params = view.lparams

            if (newChildHorizontalAlign != null) {
                params.width = when (newChildHorizontalAlign) {
                    Align.Stretch -> LayoutParams.MATCH_PARENT
                    else -> LayoutParams.WRAP_CONTENT
                }
            }
            if (newChildVerticalAlign != null) {
                params.height = when (newChildVerticalAlign) {
                    Align.Stretch -> LayoutParams.MATCH_PARENT
                    else -> LayoutParams.WRAP_CONTENT
                }
            }


            val horizontalGravity = when (view.lastSetHorizontalAlign) {
                Align.Start -> android.view.Gravity.START
                Align.Center -> android.view.Gravity.CENTER_HORIZONTAL
                Align.End -> android.view.Gravity.END
                else -> android.view.Gravity.CENTER_HORIZONTAL
            }
            val verticalGravity = when (view.lastSetVerticalAlign) {
                Align.Start -> android.view.Gravity.TOP
                Align.Center -> android.view.Gravity.CENTER_VERTICAL
                Align.End -> android.view.Gravity.BOTTOM
                else -> android.view.Gravity.CENTER_VERTICAL
            }

            if (params is com.lightningkite.kiteui.views.direct.SimplifiedLinearLayoutLayoutParams)
                params.gravity = horizontalGravity or verticalGravity
            else if (params is FrameLayout.LayoutParams)
                params.gravity = horizontalGravity or verticalGravity
            else if (params is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)
                params.gravity = horizontalGravity or verticalGravity
        }

        (native as ViewGroup).addView(view.native, index)
        if (fullyStarted) ViewCompat.requestApplyInsets(view.native)
        if ((native as ViewGroup).childCount != children.size) throw IllegalStateException("internalAddChild($index $view) failed on $this: Native child count ${(native as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
    }

    actual override fun internalRemoveChild(index: Int) {
        if ((native as ViewGroup).childCount != children.size) throw IllegalStateException("internalRemoveChild($index) failed on $this: Native child count ${(native as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
        (native as ViewGroup).removeViewAt(index)
    }

    actual override fun internalClearChildren() {
        if ((native as ViewGroup).childCount != children.size) throw IllegalStateException("internalClearChildren() failed on $this: Native child count ${(native as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
        (native as ViewGroup).removeAllViews()
    }

    protected fun getBackgroundWithRipple(
        theme: Theme,
        fullyApply: Boolean,
        oldRippleDrawable: RippleDrawable?
    ): RippleDrawable {
        val rippleColor = ColorStateList.valueOf(theme[HoverSemantic].theme.background.colorInt())
        val backgroundDrawable = if (fullyApply) {
            theme.backgroundDrawableWithoutCorners(oldRippleDrawable?.getDrawable(0) as? GradientDrawable).also {
                removeListener?.invoke()
                removeListener = it.applyGradientRadiusListener(native)
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                colors = intArrayOf(
                    theme.background.applyAlpha(0.01f).colorInt(),
                    theme.background.applyAlpha(0.01f).colorInt()
                )
            }
        }
        backgroundBlock = backgroundDrawable
        if (oldRippleDrawable != null) {
            oldRippleDrawable.setColor(rippleColor)
            // Use reflection to set the drawable to avoid API level issues
            try {
                val method = RippleDrawable::class.java.getMethod(
                    "setDrawable",
                    Int::class.javaPrimitiveType,
                    Drawable::class.java
                )
                method.invoke(oldRippleDrawable, 0, backgroundDrawable)
            } catch (e: Exception) {
                // Fallback to creating a new RippleDrawable
                return RippleDrawable(rippleColor, backgroundDrawable, null)
            }
            return oldRippleDrawable
        } else {
            return RippleDrawable(rippleColor, backgroundDrawable, null)
        }
    }

    protected fun applyThemeWithRipple(theme: ThemeAndBack) {
        val shadows = theme.theme.shadows

        if (theme.drawBackground && shadows != null && shadows.isNotEmpty()) {
            // Use neumorphic/multi-shadow rendering (ripple not used with neumorphism)
            // For neumorphism, the pressed state is indicated by concave shadows, not ripple
            native.elevation = 0f

            val neumorphicBg = background as? NeumorphicDrawable
            if (neumorphicBg != null) {
                neumorphicBg.setShadows(shadows)
                neumorphicBg.setBackgroundColor(theme.theme.background.colorInt())
                updateCorners()
            } else {
                val newBg = NeumorphicDrawable(
                    shadows = shadows,
                    cornerRadius = 0f,
                    backgroundColor = theme.theme.background.colorInt()
                )
                backgroundBlock = null
                background = newBg
                updateCorners()

                if (newBg.needsSoftwareLayer()) {
                    native.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                }
            }
        } else {
            // Standard elevation-based shadow rendering with ripple
            if (theme.drawBackground) {
                native.elevation = theme.theme.elevation.value
            } else {
                native.elevation = 0f
            }

            // Reset layer type if we were using software rendering
            if (background is NeumorphicDrawable) {
                native.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            }

            background = getBackgroundWithRipple(theme.theme, theme.drawBackground, background as? RippleDrawable)
            updateCorners()
        }
        updateTransform(theme.theme)
    }
}

var animationsEnabled: Boolean = true
actual val RView.areAnimationsEnabled: Boolean get() = com.lightningkite.kiteui.views.animationsEnabled
actual inline fun RView.withoutAnimation(action: () -> Unit) = native.withoutAnimation(action)
inline fun View.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        action()
    } finally {
        animationsEnabled = true
    }
}


inline fun View.debugPrint(get: () -> String) {
    if (debugMode && viewDebugTarget?.native == this)
        Log.tag("viewDebugTarget").info(get())
}