@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import kotlinx.coroutines.launch

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
    // Set by popovers that open or close on hover: called with true when the pointer enters, false when it leaves.
    internal var onHoverChange: ((Boolean) -> Unit)? by mutableStateOf(null)

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

    // Applied from Render, since a FocusRequester only works once its element is composed.
    private var focusPending by mutableStateOf(false)

    actual override fun requestFocus() {
        focusPending = true
    }

    actual override fun announceAsNewScreen(title: String) {
        paneTitle = title
    }

    // Compose positions are in pixels; KiteUI's are in dp.
    public actual fun screenRectangle(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInWindow()?.toRect()
    public actual fun parentRectangle(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInParent()?.toRect()
    private var density = 1f

    private fun androidx.compose.ui.geometry.Rect.toRect() = Rect(
        left = (left / density).toDouble(),
        top = (top / density).toDouble(),
        right = (right / density).toDouble(),
        bottom = (bottom / density).toDouble(),
    )

    /** Whether the parent lays this element out at all; hidden elements take no space. */
    internal open val takesSpace: Boolean get() = shown

    @Composable
    public fun Render(modifier: Modifier = Modifier) {
        density = LocalDensity.current.density
        if (focusPending) LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            focusPending = false
        }
        var m = modifier
            .onGloballyPositioned { coordinates = it }
            .bringIntoViewRequester(bringIntoView)
            .focusRequester(focusRequester)
        sizeConstraints?.let { m = m.sized(it) }
        // Like the web's visibility:hidden, an invisible element can't be interacted with.
        if (visible) m = m.then(inputModifier())
        if (visible) onHoverChange?.let { onHover ->
            m = m.pointerInput(onHover) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> onHover(true)
                            PointerEventType.Exit -> onHover(false)
                        }
                    }
                }
            }
        }
        // As on the web, an element takes the pointer over its bounds so it doesn't reach what's behind
        // (a dialog card over a page), unless it ignores interaction (its children still take it).
        if (visible && !ignoreInteraction) m = m.pointerInput(Unit) {}
        if (!visible || opacity != 1.0) m = m.graphicsLayer(alpha = if (visible) opacity.toFloat() else 0f)
        label?.let { m = m.semantics { contentDescription = it } }
        paneTitle?.let { m = m.semantics { paneTitle = it } }
        decoration?.let { d ->
            m = m.shadow(d.elevation, d.shape, clip = false).background(d.background, d.shape)
            if (d.outlineWidth.value > 0f) m = m.border(d.outlineWidth, d.outline, d.shape)
            if (d.clip) m = m.clip(d.shape)
        }
        scrolling?.let { m = it.applyTo(m, density) }
        val p = padding
        m = m.padding(start = p.left.toDp(), top = p.top.toDp(), end = p.right.toDp(), bottom = p.bottom.toDp())
        Content(m)
    }

    // Applied outside padding, so the whole element responds.
    protected open fun inputModifier(): Modifier = Modifier

    /** The element's own composable. It must apply [modifier] to exactly one root layout node. */
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
