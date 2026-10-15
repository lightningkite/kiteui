@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// False inside an invisible element: like the web's visibility:hidden, invisibility is inherited.
private val LocalVisible = compositionLocalOf { true }

/**
 * KiteUI keeps its own retained element tree; Compose only draws it.  Everything [Render] reads is
 * snapshot state, so when KiteUI's reactivity sets a property, only this element recomposes.
 */
public actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {

    actual override var opacity: Double by mutableStateOf(1.0)
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
    // Set by the pointer input in Render; popovers that open on hover watch it.
    internal val hovered = Signal(false)
    public open val interactions: Interactions? get() = null

    protected var currentTheme: ThemeAndBack by mutableStateOf(Theme.placeholder.withBack)
        private set
    private var decoration: Decoration? by mutableStateOf(null)
    private var padding: Edges by mutableStateOf(Edges.ZERO)
    private var paneTitle: String? by mutableStateOf(null)
    private var label: String? by mutableStateOf(null)

    private class Decoration(
        val background: Brush,
        val shape: Shape,
        val elevation: Dp,
        val outline: Brush,
        val outlineWidth: Dp,
        val clip: Boolean,
    )

    private var coordinates: LayoutCoordinates? = null
    private val bringIntoView = BringIntoViewRequester()
    private val focusRequester = FocusRequester()

    // Pixels per dp, as last composed.
    internal var density: Float by mutableStateOf(1f)
        private set

    actual override fun nativeApplyTheme(theme: ThemeAndBack) {
        currentTheme = theme
        decoration = if (theme.drawBackground) {
            @Suppress("DEPRECATION")
            val shape = theme.theme.shape(parent?.spacingForChildCornerRadii ?: 0.px)
            Decoration(
                background = theme.theme.background.toBrush(),
                shape = shape,
                elevation = theme.theme.elevation.toDp(),
                outline = theme.theme.outline.toBrush(),
                outlineWidth = theme.theme.outlineWidth.toDp(),
                // Like Android: rounded cards clip their content, square ones don't (so shadows inside aren't cut off).
                clip = theme.theme.cornerRadii.let { it !is CornerRadii.Fixed || it.value.value > 0f },
            )
        } else null
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
    public actual fun screenRectangle(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInWindow()?.toRect()
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
        // Does nothing if the element can't take focus (it's invisible, say), as on the web.
        if (focusRequested) LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            focusRequested = false
        }
        interactions?.Track()
        var m = modifier
            .onPlaced { coordinates = it }
            .bringIntoViewRequester(bringIntoView)
            .focusRequester(focusRequester)
        sizeConstraints?.let { m = m.sized(it) }
        if (takesInput) m = m.then(inputModifier()).pointerInput(Unit) {
            // Any pointer input takes the pointer over the element's bounds so, as on the web, it doesn't reach
            // what's behind (a dialog card over a page).
            try {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered.value = true
                            PointerEventType.Exit -> hovered.value = false
                        }
                    }
                }
            } finally {
                hovered.value = false
            }
        }
        if (!visible || opacity != 1.0) m = m.graphicsLayer(alpha = if (visible) opacity.toFloat() else 0f)
        if (visible) {
            label?.let { m = m.semantics { contentDescription = it } }
            paneTitle?.let { m = m.semantics { paneTitle = it } }
        } else {
            m = m.clearAndSetSemantics {}
        }
        decoration?.let { d ->
            m = m.shadow(d.elevation, d.shape, clip = false).background(d.background, d.shape)
            if (d.outlineWidth.value > 0f) m = m.border(d.outlineWidth, d.outline, d.shape)
            if (d.clip) m = m.clip(d.shape)
        }
        scrolling?.let { m = it.applyTo(m, takesInput) }
        val p = padding
        m = m.padding(start = p.left.toDp(), top = p.top.toDp(), end = p.right.toDp(), bottom = p.bottom.toDp())
        Content(m)
    }

    // Applied outside padding, so the whole element responds.
    protected open fun inputModifier(): Modifier = Modifier

    // It must apply its modifier to exactly one root layout node.
    @Composable
    protected abstract fun Content(modifier: Modifier)
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
        // Derive the unconstrained axis from the constrained one.
        own = when {
            own.hasBoundedWidth && (height == null) -> {
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
