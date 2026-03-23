@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.DragShadow
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.applyAlpha
import com.lightningkite.kiteui.models.coerceAtLeast
import com.lightningkite.kiteui.models.div
import com.lightningkite.kiteui.models.plus
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.direct.CoordinatorFrame
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.colorInt
import com.lightningkite.reactive.context.onRemove
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.text.toInt

actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {
    abstract val native: View

    var removeListener: (() -> Unit)? = null
    init {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw Exception("Cannot create views on any thread but the main thread")

        onRemove {
            removeListener?.invoke()
        }
    }

    open fun defaultLayoutParams(): LayoutParams =
        FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    actual override var opacity: Double = 1.0
        set(value) {
            field = value
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

    actual override var shown: Boolean = true
        set(value) {
            field = value
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

    actual override var visible: Boolean = true
        set(value) {
            field = value
            native.visibility = if (value) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }

    actual override var ignoreInteraction: Boolean = false
        set(value) {
            field = value
            native.isClickable = !value
            native.isFocusable = !value
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
    actual override var dragData: DragData? = null
        set(value) {
            field = value
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

    actual override var dropTargetDelegate: DropTargetDelegate? = null
        set(value) {
            field = value
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


    actual override fun refreshPadding() {
        val value = appliedPadding
        native.setPadding(
            value.left.value.toInt(),
            value.top.value.toInt(),
            value.right.value.toInt(),
            value.bottom.value.toInt(),
        )
    }

    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        generateSequence(native) {
            it.parent as? View
        }.firstOrNull {
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

    actual fun screenRectangle(): Rect? {
        val r = android.graphics.Rect()
        native.getGlobalVisibleRect(r)
        return Rect(
            left = r.left.toDouble(),
            top = r.top.toDouble(),
            right = r.right.toDouble(),
            bottom = r.bottom.toDouble(),
        )
    }
    actual fun parentRectangle(): Rect? {
        return Rect(
            left = native.left.toDouble(),
            top = native.top.toDouble(),
            right = native.right.toDouble(),
            bottom = native.bottom.toDouble(),
        )
    }

    protected var background: Drawable? = null
        set(value) {
            field = value
            native.background = value
        }
    protected var backgroundBlock: GradientDrawable? = null

    fun updateCorners() {
        @Suppress("DEPRECATION")
        val cr = when (val it = theme.cornerRadii) {
            is CornerRadii.AdaptiveToSpacing -> min((parent?.spacingForChildCornerRadii ?: 0.px).value, it.value.value)
            is CornerRadii.Fixed -> it.value.value
            is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(native.width, native.height)
            is CornerRadii.RatioOfSpacing -> it.value * (parent?.spacingForChildCornerRadii ?: 0.px).value
            is CornerRadii.PerCorner -> it.value.value
        }

        val asPerCorner = theme.cornerRadii as? CornerRadii.PerCorner
        val topLeft = if (asPerCorner?.topLeft != false) cr else 0f
        val topRight = if (asPerCorner?.topRight != false) cr else 0f
        val bottomRight = if (asPerCorner?.bottomRight != false) cr else 0f
        val bottomLeft = if (asPerCorner?.bottomLeft != false) cr else 0f

        val radii = floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)

        backgroundBlock?.cornerRadii = radii

        // When a view has corner radii and draws a background, clip children to the
        // rounded outline. This matches web behavior where border-radius + overflow: hidden
        // clips content (e.g. images inside a rounded frame).
        // We use Outline.setPath() with the per-corner radii array so PerCorner is respected.
        // A rounded rect path is always convex, so this works on API 21+.
        if (cr > 0f && themeAndBack.drawBackground) {
            val capturedRadii = radii.copyOf()
            native.outlineProvider = object : ViewOutlineProvider() {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun getOutline(view: View, outline: Outline) {
                    val path = Path().apply {
                        addRoundRect(
                            RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
                            capturedRadii,
                            Path.Direction.CW
                        )
                    }
                    outline.setPath(path)
                }
            }
            native.clipToOutline = true
        } else if (!native.clipToOutline) {
            native.outlineProvider = ViewOutlineProvider.BACKGROUND
        }
    }

    // Map to track active animators for each view property
    companion object {
        private val activeAnimators = mutableMapOf<String, ValueAnimator>()
        // by Claude - cache reflected Method to avoid repeated getMethod() calls on every clickable element
        private val rippleSetDrawableMethod: java.lang.reflect.Method? by lazy {
            try {
                RippleDrawable::class.java.getMethod(
                    "setDrawable",
                    Int::class.javaPrimitiveType,
                    Drawable::class.java
                )
            } catch (e: Exception) {
                null
            }
        }
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

    actual override fun nativeApplyTheme(theme: ThemeAndBack) {
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        if (theme.drawBackground) {
            val backgroundDrawable = theme.theme.backgroundDrawableWithoutCorners(background as? GradientDrawable).also {
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

    override fun startup() {
        super.startup()
        // Block touches below
        val wasClickable = native.isClickable
        val wasFocusable = native.isFocusable
        val hasInteractiveParent =
            generateSequence(this as Element) { it.parent }.any { it.native.isClickable || it.native.isFocusable }
        debugPrint {
            buildString {
                appendLine("--postsetup--")
                appendLine("hasInteractiveParent: $hasInteractiveParent")
                appendLine("interactive parent is: ${generateSequence(this@NativeElement as Element) { it.parent }.find { it.native.isClickable || it.native.isFocusable }}")
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
            // by Claude - use cached reflected Method to avoid repeated getMethod() lookup
            val method = rippleSetDrawableMethod
            if (method != null) {
                try {
                    method.invoke(oldRippleDrawable, 0, backgroundDrawable)
                } catch (e: Exception) {
                    return RippleDrawable(rippleColor, backgroundDrawable, null)
                }
            } else {
                return RippleDrawable(rippleColor, backgroundDrawable, null)
            }
            return oldRippleDrawable
        } else {
            return RippleDrawable(rippleColor, backgroundDrawable, null)
        }
    }

    protected fun applyThemeWithRipple(theme: ThemeAndBack) {
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        background = getBackgroundWithRipple(theme.theme, theme.drawBackground, background as? RippleDrawable)
        updateCorners()
        updateTransform(theme.theme)
    }

    // printing only works on web anyway.
    actual override var showOnPrint: Boolean = true
}

val Element.native get() = underlyingNativeElement.native