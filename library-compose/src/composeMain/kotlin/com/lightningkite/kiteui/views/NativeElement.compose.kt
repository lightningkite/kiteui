@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.NativeLinearLayoutElement
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.graphics.Color as ComposeColor

// False inside an invisible element: like the web's visibility:hidden, invisibility is inherited.
internal val LocalVisible = compositionLocalOf { true }

/**
 * KiteUI keeps its own retained element tree; Compose only draws it.  Everything [Render] reads is
 * snapshot state, so when KiteUI's reactivity sets a property, only this element recomposes.
 */
public actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {

    private var targetOpacity: Double by mutableStateOf(1.0)
    actual override var opacity: Double
        get() = targetOpacity
        set(value) {
            val from = targetOpacity
            targetOpacity = value
            changeAppearance(currentTheme, from)
        }
    actual override var shown: Boolean by mutableStateOf(true)
    actual override var visible: Boolean by mutableStateOf(true)
    actual override var ignoreInteraction: Boolean by mutableStateOf(false)
    // TODO: drag and drop.
    actual override var dragData: DragData? = null
    actual override var dropTargetDelegate: DropTargetDelegate? = null
    actual override var showOnPrint: Boolean = true

    // Set by modifiers during setup and read by the parent's layout. A null alignment means stretch.
    public var horizontalAlign: Align? by mutableStateOf(null)
    public var verticalAlign: Align? by mutableStateOf(null)
    public var weight: Float? by mutableStateOf(null)
    public var sizeConstraints: SizeConstraints? by mutableStateOf(null)
    internal var scrolling: ScrollingBehaviorImpl? = null
    // Set by shownWhen during setup.  Only these elements can collapse, so only they get the modifiers for it.
    internal var collapsible: Boolean = false
    private var watchesWindow = false
    // Set by HoverNode; popovers that open on hover watch it.
    internal val hovered = Signal(false)
    public open val interactions: Interactions? get() = null

    protected var currentTheme: ThemeAndBack by mutableStateOf(Theme.placeholder.withBack)
        private set
    private var decoration: Decoration? by mutableStateOf(null)
    private var padding: Edges by mutableStateOf(Edges.ZERO)
    private var paneTitle: String? by mutableStateOf(null)
    private var label: String? by mutableStateOf(null)

    private class Decoration(
        val background: Paint,
        val shape: Shape,
        val outline: Paint,
        val outlineWidth: Dp,
        val clip: Boolean,
    )

    private fun decorationOf(theme: ThemeAndBack): Decoration? = if (theme.drawBackground) {
        @Suppress("DEPRECATION")
        val shape = theme.theme.shape(parent?.spacingForChildCornerRadii ?: 0.px)
        Decoration(
            background = theme.theme.background,
            shape = shape,
            outline = theme.theme.outline,
            outlineWidth = theme.theme.outlineWidth.toDp(),
            // Like Android: rounded cards clip their content, square ones don't (so shadows inside aren't cut off).
            clip = theme.theme.cornerRadii.let { it !is CornerRadii.Fixed || it.value.value > 0f },
        )
    } else null

    // State so that a layout placing something against this element (a popover) runs again when it leaves the screen.
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null)
        private set
    private val bringIntoView = BringIntoViewRequester()
    private val focusRequester = FocusRequester()

    // Pixels per dp, as last composed.
    internal var density: Float by mutableStateOf(1f)
        private set

    actual override fun nativeApplyTheme(theme: ThemeAndBack) {
        val from = currentTheme
        decoration?.let { lastDecoration = it }
        currentTheme = theme
        decoration = decorationOf(theme)
        changeAppearance(from, targetOpacity)
    }

    // Kept when the background goes, so that it can fade out.
    private var lastDecoration: Decoration? = null

    /**
     * Theme and opacity changes animate, as the web's CSS transitions do.  Render reads these while drawing, so
     * frames only redraw.  Created by the first change that animates; null means everything is at its target.
     */
    private class AppearanceAnimation(theme: ThemeAndBack, opacity: Double) {
        val background = Animatable(theme.backgroundColor)
        val outline = Animatable(theme.outlineColor)
        val foreground = Animatable(theme.foregroundColor)
        val alpha = Animatable(opacity.toFloat())
        val elevation = Animatable(theme.elevation, Dp.VectorConverter)
        val translation = Animatable(theme.transformation.translation, Offset.VectorConverter)
        val scale = Animatable(theme.transformation.scale, Offset.VectorConverter)
        val rotation = Animatable(theme.transformation.rotation.toFloat())
        var job: Job? = null
    }

    private var appearance: AppearanceAnimation? by mutableStateOf(null)

    // Changes animate once the element is on screen, except inside withoutAnimation.
    private fun changeAppearance(fromTheme: ThemeAndBack, fromOpacity: Double) {
        if (fromTheme == currentTheme && fromOpacity == targetOpacity) return
        appearance?.job?.cancel()
        val context = animationContext
        if (context == null || !animationsEnabled || coordinates?.isAttached != true) {
            appearance = null
            return
        }
        val animation = appearance ?: AppearanceAnimation(fromTheme, fromOpacity).also { appearance = it }
        val theme = currentTheme
        val opacity = targetOpacity.toFloat()
        val duration = theme.theme.transitionDuration.inWholeMilliseconds.toInt()
        animation.job = launch(context) {
            coroutineScope {
                // Linear, like the web's.
                fun <T, V : AnimationVector> Animatable<T, V>.animate(target: T) {
                    if (value != target) launch { animateTo(target, tween(duration, easing = LinearEasing)) }
                }
                animation.background.animate(theme.backgroundColor)
                animation.outline.animate(theme.outlineColor)
                animation.foreground.animate(theme.foregroundColor)
                animation.alpha.animate(opacity)
                animation.elevation.animate(theme.elevation)
                animation.translation.animate(theme.transformation.translation)
                animation.scale.animate(theme.transformation.scale)
                animation.rotation.animate(theme.transformation.rotation.toFloat())
            }
            appearance = null
            lastDecoration = null
        }
    }

    /** The theme's foreground colour, animated by theme transitions.  Read while drawing, it animates without recomposing. */
    protected val foreground: ComposeColor
        get() = appearance?.foreground?.value ?: currentTheme.foregroundColor

    // An animateIn, animateOut or shownWhen change.  A finished animateOut stays, holding the exit pose.
    private class ScreenAnimation(val transition: ScreenTransition, val entering: Boolean, val collapses: Boolean, start: Float) {
        // 0 at rest, 1 in the entry or exit pose (and, for shownWhen, collapsed).
        val away = Animatable(start)
        var job: Job? = null
    }

    private var screenAnimation: ScreenAnimation? by mutableStateOf(null)

    /**
     * How far a shownWhen change has collapsed this element along its linear parent's axis, from 0 to 1.  An
     * overshooting easing doesn't take the size beyond either end.
     */
    internal val collapsedFraction: Float get() = screenAnimation?.takeIf { it.collapses }?.away?.value?.coerceIn(0f, 1f) ?: 0f

    /**
     * Animates to rest ([entering]) or to the exit pose, calling [done] at the end unless another animation
     * takes over first.  [collapses] also shrinks the element along its parent's axis (the parent must be a
     * linear layout), so its siblings move smoothly.
     */
    internal fun animateScreenTransition(transition: ScreenTransition, entering: Boolean, collapses: Boolean, done: (() -> Unit)?) {
        val previous = screenAnimation
        previous?.job?.cancel()
        val context = animationContext
        val moves = transition.fade || transition.entryTransform != Transformation() || transition.exitTransform != Transformation()
        if (context == null || !(moves || collapses)) {
            screenAnimation = null
            done?.invoke()
            return
        }
        // Takes over from where an interrupted animation got to, as the web does.
        val animation = ScreenAnimation(transition, entering, collapses, start = previous?.away?.value ?: if (entering) 1f else 0f)
        screenAnimation = animation
        val easing = transition.easing.let { CubicBezierEasing(it.x1, it.y1, it.x2, it.y2) }
        animation.job = launch(context) {
            animation.away.animateTo(if (entering) 0f else 1f, tween(theme.transitionDuration.inWholeMilliseconds.toInt(), easing = easing))
            if (entering) screenAnimation = null
            done?.invoke()
        }
    }

    // While dynamicWeight animates: the weight so far, and how much of its natural size the element keeps (all
    // of it at weight 0, none at a positive weight).  They animate together, so weight 0 blends into a positive one.
    private class WeightAnimation(weight: Float, naturalPart: Float) {
        val weight = Animatable(weight)
        val naturalPart = Animatable(naturalPart)
    }

    private var weightAnimation: WeightAnimation? by mutableStateOf(null)
    private var weightJob: Job? = null

    internal val animatedWeight: Float get() = weightAnimation?.weight?.value ?: weight ?: 0f
    internal val naturalPart: Float get() = weightAnimation?.naturalPart?.value ?: if ((weight ?: 0f) > 0f) 0f else 1f

    /** Sets [weight], animating from the current one once the element has started, except inside withoutAnimation. */
    internal fun changeWeight(to: Float) {
        if (to == weight) return
        weightJob?.cancel()
        val context = animationContext
        if (weight == null || context == null || !animationsEnabled || !fullyStarted) {
            weightAnimation = null
            weight = to
            return
        }
        val animation = weightAnimation ?: WeightAnimation(animatedWeight, naturalPart)
        weightAnimation = animation
        weight = to
        // Linear, like the web's.
        val spec = tween<Float>(theme.transitionDuration.inWholeMilliseconds.toInt(), easing = LinearEasing)
        weightJob = launch(context) {
            coroutineScope {
                launch { animation.weight.animateTo(to, spec) }
                animation.naturalPart.animateTo(if (to > 0f) 0f else 1f, spec)
            }
            weightAnimation = null
        }
    }

    actual override fun refreshPadding() {
        padding = appliedPadding
    }

    override var accessibleLabel: String?
        get() = super.accessibleLabel
        set(value) {
            super.accessibleLabel = value
            label = value
        }

    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        launch { bringIntoView.bringIntoView() }
    }

    // Applied by Render, since a FocusRequester only works once its element is composed.  Like the web, which
    // focuses a moment later, a request lapses if the element isn't on screen by then (it's not shown).
    private var focusRequested by mutableStateOf(false)
    private var focusRequestLapse: Job? = null

    actual override fun requestFocus() {
        focusRequested = true
        focusRequestLapse?.cancel()
        focusRequestLapse = launch {
            delay(100.milliseconds)
            focusRequested = false
        }
    }

    actual override fun announceAsNewScreen(title: String) {
        paneTitle = title
    }

    // Compose positions are in pixels; KiteUI's are in dp.
    // Unclipped, like the web's getBoundingClientRect: a partly scrolled-out element keeps its whole size.
    public actual fun screenRectangle(): Rect? = coordinates?.takeIf { it.isAttached }
        ?.let { androidx.compose.ui.geometry.Rect(it.positionInWindow(), it.size.toSize()) }?.toRect()
    public actual fun parentRectangle(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInParent()?.toRect()

    private fun androidx.compose.ui.geometry.Rect.toRect() = Rect(
        left = (left / density).toDouble(),
        top = (top / density).toDouble(),
        right = (right / density).toDouble(),
        bottom = (bottom / density).toDouble(),
    )

    // Hidden elements take no space.
    internal open val takesSpace: Boolean get() = shown

    // Whether this element takes pointer, key and focus input itself: not when it or an ancestor is invisible
    // (the web's visibility:hidden), nor when it ignores interaction (its children still may).
    protected val takesInput: Boolean
        @Composable get() = LocalVisible.current && !ignoreInteraction

    @Composable
    public fun Render(modifier: Modifier = Modifier): Unit = CompositionLocalProvider(LocalVisible provides (visible && LocalVisible.current)) {
        val density = LocalDensity.current.density
        SideEffect { this.density = density }
        // The coordinates lead to this composition's layout nodes, which must not outlive it.
        DisposableEffect(Unit) { onDispose { coordinates = null } }
        // Does nothing if the element can't take focus (it's invisible, say), as on the web.
        if (focusRequested) LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            focusRequested = false
        }
        interactions?.Track()
        // Changes to an element (its theme, hovering, visibility, animations) change its modifiers' values, never
        // which modifiers it has: that is decided at setup, apart from rare changes like sizing.  Compose UI 1.12.1
        // keeps references to the modifier lists of a node's last change in the chain's shape (adding or removing a
        // modifier), and those lists are reused by other nodes, whose modifiers (and so elements, and their
        // subtrees) then outlive them.  See the plan's known issues and PureComposeRetentionTest.
        var m = modifier
            .onPlaced { coordinates = it }
            .bringIntoViewRequester(bringIntoView)
            .focusRequester(focusRequester)
        if (collapsible) m = m
            .drawWithContent {
                if (screenAnimation?.collapses == true) clipRect { this@drawWithContent.drawContent() } else drawContent()
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val shown = 1f - collapsedFraction
                if (shown == 1f) return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                val vertical = (parent as NativeLinearLayoutElement).layoutVertical
                // A weighted element's size is fixed by the parent, which shrinks its weight instead.
                val width = if (!vertical && !constraints.hasFixedWidth) (placeable.width * shown).roundToInt() else placeable.width
                val height = if (vertical && !constraints.hasFixedHeight) (placeable.height * shown).roundToInt() else placeable.height
                layout(width, height) { placeable.place(0, 0) }
            }
        sizeConstraints?.let { m = m.sized(it) }
        val takesInput = takesInput
        m = m.then(inputModifier(takesInput)).then(HoverElement(this, takesInput))
        val theme = currentTheme
        val a = appearance
        // Losing its background, an element keeps drawing the old one while it fades.
        val d = decoration ?: lastDecoration?.takeIf { a != null }
        val transform = theme.transformation
        val visible = visible
        val clip = d != null && d.clip && decoration != null
        m = m.graphicsLayer {
            // A screen transition's pose and the theme's transform are combined simply, which is exact when only one
            // of them moves the element, as is usual.
            val screen = screenAnimation
            val away = screen?.away?.value ?: 0f
            val pose = (if (screen?.entering == true) screen.transition.entryTransform else screen?.transition?.exitTransform) ?: Transformation()
            val fade = if (screen?.transition?.fade == true) 1f - away else 1f
            alpha = fade * if (visible) a?.alpha?.value ?: targetOpacity.toFloat() else 0f
            shadowElevation = (a?.elevation?.value ?: theme.elevation).toPx()
            shape = d?.shape ?: RectangleShape
            this.clip = clip
            val t = a?.translation?.value ?: transform.translation
            // The pose's translation is a fraction of the element's own size, like CSS's translate(%).
            translationX = t.x.dp.toPx() + (pose.translationX * size.width * away).toFloat()
            translationY = t.y.dp.toPx() + (pose.translationY * size.height * away).toFloat()
            val s = a?.scale?.value ?: transform.scale
            scaleX = s.x * lerp(1f, pose.scaleX.toFloat(), away)
            scaleY = s.y * lerp(1f, pose.scaleY.toFloat(), away)
            rotationZ = (a?.rotation?.value ?: transform.rotation.toFloat()) + (pose.rotation * away).toFloat()
            rotationX = (transform.rotationX + pose.rotationX * away).toFloat()
            rotationY = (transform.rotationY + pose.rotationY * away).toFloat()
        }
        var inWindow by remember { mutableStateOf(false) }
        // Watching every element would slow scrolling, so only those that have faded do, from then on.
        if (d?.background is FadingColor) watchesWindow = true
        if (watchesWindow) m = m.onVisibilityChanged(minFractionVisible = 0f) { inWindow = it }
        val fading = (d?.background as? FadingColor)?.let {
            // There and back in 2s, as on the web, while it can be seen.
            if (inWindow && LocalVisible.current && targetOpacity > 0.0) {
                rememberInfiniteTransition().animateColor(it.base.toCompose(), it.alternate.toCompose(), infiniteRepeatable(tween(1000), RepeatMode.Reverse))
            } else null
        }
        m = m.drawWithCache {
            if (d == null) return@drawWithCache onDrawWithContent { drawContent() }
            val backgroundBrush = d.background.toBrush()
            val outlineBrush = d.outline.toBrush()
            val shape = d.shape.createOutline(size, layoutDirection, this)
            val width = d.outlineWidth.toPx()
            // Inset by half the stroke, so that like Modifier.border's it lies inside the bounds.
            val border = d.shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
            onDrawWithContent {
                // Plain colours animate; gradients change at once.
                when {
                    fading != null -> drawOutline(shape, fading.value)
                    a != null && d.background is Color -> drawOutline(shape, a.background.value)
                    else -> drawOutline(shape, backgroundBrush)
                }
                drawContent()
                if (width > 0f) translate(width / 2, width / 2) {
                    if (a != null && d.outline is Color) drawOutline(border, a.outline.value, style = Stroke(width))
                    else drawOutline(border, outlineBrush, style = Stroke(width))
                }
            }
        }
        scrolling?.let { m = it.applyTo(m, takesInput) }
        val p = padding
        m = m.padding(start = p.left.toDp(), top = p.top.toDp(), end = p.right.toDp(), bottom = p.bottom.toDp())
        // Last, so that a view's own semantics still give the node its bounds (the view's, inside the padding).
        val label = label
        val paneTitle = paneTitle
        m = m.semantics {
            if (visible) {
                label?.let { contentDescription = it }
                paneTitle?.let { this.paneTitle = it }
            } else {
                hideFromAccessibility()
            }
        }
        Content(m)
    }

    // Applied outside padding, so the whole element responds.  Disabled rather than left out when the element
    // doesn't take input, so that the modifiers keep their shape (see Render).
    protected open fun inputModifier(takesInput: Boolean): Modifier = Modifier

    // It must apply its modifier to exactly one root layout node.
    @Composable
    protected abstract fun Content(modifier: Modifier)
}

// Any pointer input node takes the pointer over the element's bounds so, as on the web, it doesn't reach what's
// behind (a dialog card over a page).  So, like Compose's clickable, this only has one while it's enabled.
private data class HoverElement(val element: NativeElement, val enabled: Boolean) : ModifierNodeElement<HoverNode>() {
    override fun create() = HoverNode(element, enabled)
    override fun update(node: HoverNode) = node.update(element, enabled)
}

private class HoverNode(private var element: NativeElement, enabled: Boolean) : DelegatingNode() {
    private var tracking: Tracking? = if (enabled) delegate(Tracking()) else null

    fun update(element: NativeElement, enabled: Boolean) {
        this.element = element
        val tracking = tracking
        if (enabled && tracking == null) this.tracking = delegate(Tracking())
        if (!enabled && tracking != null) {
            undelegate(tracking)
            this.tracking = null
        }
    }

    private inner class Tracking : Modifier.Node(), PointerInputModifierNode {
        override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
            if (pass != PointerEventPass.Main) return
            when (pointerEvent.type) {
                PointerEventType.Enter -> element.hovered.value = true
                PointerEventType.Exit -> element.hovered.value = false
            }
        }

        override fun onCancelPointerInput() {
            element.hovered.value = false
        }

        override fun onDetach() {
            element.hovered.value = false
        }
    }
}

/**
 * Explicit sizes are capped by the space the parent has, as Android does.  When the parent asks for more
 * (a stretched slot), Compose centers the smaller result in it, which also matches Android.
 */
private fun Modifier.sized(c: SizeConstraints): Modifier = layout { measurable, incoming ->
    fun Dimension?.toPx(): Int? = this?.let { it.value.dp.roundToPx() }
    val width = c.width.toPx()?.coerceAtMost(incoming.maxWidth)
    val height = c.height.toPx()?.coerceAtMost(incoming.maxHeight)
    val maxWidth = width ?: minOf(incoming.maxWidth, c.maxWidth.toPx() ?: Constraints.Infinity)
    val maxHeight = height ?: minOf(incoming.maxHeight, c.maxHeight.toPx() ?: Constraints.Infinity)
    var own = Constraints(
        minWidth = width ?: maxOf(incoming.minWidth, c.minWidth.toPx() ?: 0).coerceAtMost(maxWidth),
        maxWidth = maxWidth,
        minHeight = height ?: maxOf(incoming.minHeight, c.minHeight.toPx() ?: 0).coerceAtMost(maxHeight),
        maxHeight = maxHeight,
    )
    c.aspectRatio?.let { ratio ->
        // Derive one axis from the other: from the height when only it is fixed (explicitly, or stretched by a row,
        // as on Android), otherwise from the width if it's bounded.
        val fromHeight = (own.hasFixedHeight && !own.hasFixedWidth) || !own.hasBoundedWidth
        own = when {
            !fromHeight -> {
                val w = own.maxWidth
                val h = (w / ratio).toInt().coerceIn(own.minHeight, own.maxHeight)
                own.copy(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h)
            }
            own.hasBoundedHeight -> {
                val h = own.maxHeight
                val w = (h * ratio).toInt().coerceIn(own.minWidth, own.maxWidth)
                own.copy(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h)
            }
            else -> own
        }
    }
    val placeable = measurable.measure(own)
    // Coerced because intrinsic measurement reports the content's own size, ignoring explicit ones.
    layout(own.constrainWidth(placeable.width), own.constrainHeight(placeable.height)) { placeable.place(0, 0) }
}

private val Float.dp: Dp get() = Dp(this)

// What theme transitions animate.  Without a background, a theme's colours are transparent (so they fade in place
// rather than through black), and it has no elevation or transform, as on the web.
private val ThemeAndBack.backgroundColor: ComposeColor get() = theme.background.closestColor().toCompose().let { if (drawBackground) it else it.copy(alpha = 0f) }
private val ThemeAndBack.outlineColor: ComposeColor get() = theme.outline.closestColor().toCompose().let { if (drawBackground) it else it.copy(alpha = 0f) }
private val ThemeAndBack.foregroundColor: ComposeColor get() = theme.foreground.closestColor().toCompose()
private val ThemeAndBack.elevation: Dp get() = if (drawBackground) theme.elevation.toDp() else Dp(0f)
private val ThemeAndBack.transformation: Transformation get() = theme.transform?.takeIf { drawBackground } ?: Transformation()
private val Transformation.translation: Offset get() = Offset(translationX.toFloat(), translationY.toFloat())
private val Transformation.scale: Offset get() = Offset(scaleX.toFloat(), scaleY.toFloat())
