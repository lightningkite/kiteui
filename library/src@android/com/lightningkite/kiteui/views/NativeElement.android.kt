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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.DragShadow
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.LiveRegionMode
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.applyAlpha
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.colorInt
import com.lightningkite.reactive.context.onRemove
import kotlin.math.min
import kotlin.math.roundToInt

public actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {
    public abstract val native: View

    internal var removeListener: (() -> Unit)? = null
    init {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw Exception("Cannot create views on any thread but the main thread")

        onRemove {
            removeListener?.invoke()
        }
    }

    public open fun defaultLayoutParams(): LayoutParams =
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

    override var accessibleLabel: String?
        get() = super.accessibleLabel
        set(value) {
            super.accessibleLabel = value
            native.contentDescription = value
        }

    override var accessibleLiveRegion: LiveRegionMode
        get() = super.accessibleLiveRegion
        set(value) {
            super.accessibleLiveRegion = value
            native.accessibilityLiveRegion = when (value) {
                LiveRegionMode.None -> View.ACCESSIBILITY_LIVE_REGION_NONE
                LiveRegionMode.Polite -> View.ACCESSIBILITY_LIVE_REGION_POLITE
                LiveRegionMode.Assertive -> View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
            }
        }

    override var labelFor: Element?
        get() = super.labelFor
        set(value) {
            super.labelFor = value
            if (value != null) {
                val targetView = value.underlyingNativeElement.native
                if (targetView.id == View.NO_ID) {
                    targetView.id = View.generateViewId()
                }
                native.labelFor = targetView.id
            } else {
                native.labelFor = View.NO_ID
            }
        }

    override var describedBy: Element?
        get() = super.describedBy
        set(value) {
            super.describedBy = value
            // Android doesn't have a direct describedBy API. The association is stored
            // for use by components (e.g., errorText/issueText) that announce changes.
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

    /**
     * Where in this view the current gesture went down, in view coordinates, or null if no touch
     * has been seen.
     *
     * Translated from [KiteUiActivity.lastTouchDownOnScreen] rather than recorded here: a `View`'s
     * own `OnTouchListener` runs only after child dispatch, so a row containing anything that
     * consumes touches - a plain text view does - would never see the press that started its own
     * drag. The activity sees every gesture first, unconditionally.
     *
     * internal rather than private so a test can confirm the translation, which is otherwise only
     * observable inside a drag shadow the platform has already taken ownership of.
     */
    internal val grabPoint: Point?
        get() {
            val screen = KiteUiActivity.lastTouchDownOnScreen ?: return null
            val viewOnScreen = IntArray(2).also(native::getLocationOnScreen)
            return Point(screen.x - viewOnScreen[0], screen.y - viewOnScreen[1])
        }

    /**
     * Anchors the drag shadow at the point the view was grabbed.
     *
     * The platform's own `View.DragShadowBuilder` puts the touch point at the centre of the shadow,
     * so the row jumps to centre itself under the finger the instant a drag begins, and every later
     * position is offset by however far from the middle it was picked up.
     */
    internal class GrabPointShadowBuilder(view: View, private val grab: Point) : View.DragShadowBuilder(view) {
        override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
            val v = view ?: return
            // A zero-size shadow is rejected by the platform, hence the floor of 1.
            outShadowSize?.set(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1))
            outShadowTouchPoint?.set(
                grab.x.coerceIn(0, v.width.coerceAtLeast(1)),
                grab.y.coerceIn(0, v.height.coerceAtLeast(1)),
            )
        }
    }

    // drag 'n drop
    actual override var dragData: DragData? = null
        set(value) {
            field = value
            if (value == null) {
                native.setOnLongClickListener(null)
            } else {
                native.setOnLongClickListener {
                    val clipData = ClipData(value.label, arrayOf(value.mimeType), ClipData.Item(value.data))
                    val shadowBuilder = value.dragShadow?.let(::DragShadowBuilder)
                        ?: grabPoint?.let { GrabPointShadowBuilder(native, it) }
                        // No recorded touch means the drag was not started by one - the platform's
                        // centred shadow is as good a guess as any.
                        ?: View.DragShadowBuilder(native)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        native.startDragAndDrop(clipData, shadowBuilder, value, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        native.startDrag(clipData, shadowBuilder, value, 0)
                    }
                    true
                }
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

    actual override fun announceAsNewScreen(title: String) {
        afterTimeout(16) {
            // A pane is Android's model for a region that gets swapped out while the window stays
            // put; TalkBack announces the new name whenever it changes. Moving TalkBack's focus by
            // hand instead would require making this container a speakable node, which would then
            // swallow the announcements of everything inside it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) native.accessibilityPaneTitle = title
            else @Suppress("DEPRECATION") native.announceForAccessibility(title)
        }
    }

    public actual fun screenRectangle(): Rect? {
        val r = android.graphics.Rect()
        native.getGlobalVisibleRect(r)
        return Rect(
            left = r.left.toDouble(),
            top = r.top.toDouble(),
            right = r.right.toDouble(),
            bottom = r.bottom.toDouble(),
        )
    }
    public actual fun parentRectangle(): Rect? {
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

    /** Whether the corner-radius handling below turned on `clipToOutline`, so it knows to turn it off again. */
    private var appliedCornerClip: Boolean = false

    public fun updateCorners() {
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
        //
        // Which Outline shape is used decides whether clipping happens at all: View.clipToOutline
        // only clips outlines for which Outline.canClip() is true, and that excludes path-based
        // outlines below API 33 (canClip() returned `mMode != MODE_PATH` until then; setConvexPath
        // is just setPath, so neither helps). setRoundRect is clippable on every supported level,
        // so it is used whenever all four corners share a radius - which is every case except an
        // explicit CornerRadii.PerCorner that switches some corners off. Those genuinely cannot be
        // expressed as a round rect and fall back to the path, which still gives a correct shadow
        // everywhere and correct clipping from API 33 on.
        if (cr > 0f && themeAndBack.drawBackground) {
            val uniformRadius = radii.all { it == radii[0] }
            val capturedRadii = radii.copyOf()
            native.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    if (uniformRadius) {
                        // A radius larger than the view degenerates to a pill; Skia scales the
                        // path form down the same way, so clamping here keeps the two in step.
                        outline.setRoundRect(
                            0,
                            0,
                            view.width,
                            view.height,
                            capturedRadii[0].coerceAtMost(min(view.width, view.height) / 2f)
                        )
                    } else {
                        val path = Path().apply {
                            addRoundRect(
                                RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
                                capturedRadii,
                                Path.Direction.CW
                            )
                        }
                        @Suppress("DEPRECATION")
                        outline.setConvexPath(path)
                    }
                }
            }
            native.clipToOutline = true
            appliedCornerClip = true
        } else if (appliedCornerClip) {
            // Undo our own clip when the theme stops asking for one - a reactive `::theme` switching
            // from a rounded card to a flat background would otherwise leave children cut to the old
            // radius forever. Tracked with a flag rather than by reading clipToOutline, because other
            // elements (RawImageView, ProgressBar) set it on their own natives and must not be reset
            // here just because this element has no corners.
            appliedCornerClip = false
            native.clipToOutline = false
            native.outlineProvider = ViewOutlineProvider.BACKGROUND
        } else if (!native.clipToOutline) {
            native.outlineProvider = ViewOutlineProvider.BACKGROUND
        }
    }

    // Map to track active animators for each view property
    internal companion object {
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

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
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

public val Element.native: View get() = underlyingNativeElement.native