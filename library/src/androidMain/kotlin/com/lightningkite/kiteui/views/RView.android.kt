package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import com.lightningkite.kiteui.views.ImagePaintDrawable
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.lightningkite.readable.onRemove
import kotlin.math.min

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: View

    init {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw Exception("Cannot create views on any thread but the main thread")
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


    // drag 'n drop
    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            if (value == null) native.setOnLongClickListener(null)
            else native.setOnLongClickListener {
                native.startDrag(
                    ClipData(value.label, arrayOf(value.mimeType), ClipData.Item(value.data)),
                    View.DragShadowBuilder(native),
                    null,
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
                        data = event.clipData.let {
                            DragData(
                                it.description.label.toString(),
                                (0..<it.itemCount).associate { i -> it.description.getMimeType(i) to it.getItemAt(i).text.toString() },
                            )
                        },
                        xInView = event.x.toDouble(),
                        yInView = event.y.toDouble(),
                    )
                when (event.action) {
                    android.view.DragEvent.ACTION_DRAG_ENTERED, android.view.DragEvent.ACTION_DRAG_LOCATION -> value.over(
                        ev
                    )

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
            is CornerRadii.ForceConstant -> it.value.value
            is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(native.width, native.height)
            is CornerRadii.Constant -> min((parent?.mySpacingForChildren ?: 0.px).value, it.value.value)
            is CornerRadii.RatioOfSpacing -> it.value * (parent?.mySpacingForChildren ?: 0.px).value
            is CornerRadii.PerCorner -> it.value.value
        }

        val asPerCorner = theme.cornerRadii as? CornerRadii.PerCorner
        val topLeft = if (asPerCorner?.topLeft != false) cr else 0f
        val topRight = if (asPerCorner?.topRight != false) cr else 0f
        val bottomRight = if (asPerCorner?.bottomRight != false) cr else 0f
        val bottomLeft = if (asPerCorner?.bottomLeft != false) cr else 0f
        backgroundBlock?.cornerRadii =
            floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
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

    actual override fun applyTheme(theme: ThemeAndBack) {
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        if (theme.drawBackground) {
            // Check if the background is an ImagePaint
            (theme.theme.background as? ImagePaint)?.let { imagePaint ->
                val backgroundAlreadyCorrect = (background as? ImagePaintDrawable)?.let { existing ->
                    existing.strokeWidth == theme.theme.outlineWidth &&
                        existing.imagePaint == imagePaint &&
                        existing.stroke == theme.theme.outline
                } ?: false
                // Create an ImagePaintDrawable with the context from the native view
                if(!backgroundAlreadyCorrect) {
                    val drawable = ImagePaintDrawable(
                        context = native.context,
                        imagePaint = imagePaint,
                        strokeWidth = theme.theme.outlineWidth,
                        stroke = theme.theme.outline
                    )
                    backgroundBlock = drawable
                }
                // Apply corner radii
                updateCorners()
                background = backgroundBlock
            } ?: run {
                // Use the regular background drawable
                val backgroundDrawable = theme.theme.backgroundDrawableWithoutCorners(background as? GradientDrawable)
                backgroundBlock = backgroundDrawable
                updateCorners()
                background = backgroundDrawable
            }
        } else {
            backgroundBlock = null
            background = null
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
            theme.backgroundDrawableWithoutCorners(oldRippleDrawable?.getDrawable(0) as? GradientDrawable)
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
                val method = RippleDrawable::class.java.getMethod("setDrawable", Int::class.javaPrimitiveType, Drawable::class.java)
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
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        background = getBackgroundWithRipple(theme.theme, theme.drawBackground, background as? RippleDrawable)
        updateCorners()
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


inline fun View.debugPrint(get: ()->String) {
    if(debugMode && viewDebugTarget?.native == this)
        Log.tag("viewDebugTarget").info(get())
}
