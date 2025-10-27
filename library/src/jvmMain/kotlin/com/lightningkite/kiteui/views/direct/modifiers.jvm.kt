package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.core.Reactive

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWrapper {
    // TODO: Implement popover support - requires dialog/overlay system
    return ViewWrapper
}

@Deprecated(message = "Use hintPopover or opensMenu depending on your situation.")
@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    // TODO: Implement popover support - requires dialog/overlay system
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper {
    // TODO: Implement text popover - typically a tooltip
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    // TODO: Weight in Compose requires RowScope/ColumnScope context
    // For now, we use fillMaxWidth/fillMaxHeight as approximation when weight > 0
    beforeNextElementSetup {
        // Store weight in tag for potential future use
        // For now, no-op since we can't apply weight outside Row/Column scope
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper {
    // TODO: Changing weight in Compose requires RowScope/ColumnScope context
    // For now, no-op
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.align(
    horizontal: Align,
    vertical: Align
): ViewWrapper {
    wrapNextIn(object : RViewWrapper(context) {
        @Composable
        override fun compose() {
            // Convert KiteUI Align to Compose Alignment
            val alignment = when {
                horizontal == Align.Start && vertical == Align.Start -> Alignment.TopStart
                horizontal == Align.Center && vertical == Align.Start -> Alignment.TopCenter
                horizontal == Align.End && vertical == Align.Start -> Alignment.TopEnd
                horizontal == Align.Start && vertical == Align.Center -> Alignment.CenterStart
                horizontal == Align.Center && vertical == Align.Center -> Alignment.Center
                horizontal == Align.End && vertical == Align.Center -> Alignment.CenterEnd
                horizontal == Align.Start && vertical == Align.End -> Alignment.BottomStart
                horizontal == Align.Center && vertical == Align.End -> Alignment.BottomCenter
                horizontal == Align.End && vertical == Align.End -> Alignment.BottomEnd
                else -> Alignment.Center
            }

            // For stretch alignment, we need to handle it differently
            val modifier = when {
                horizontal == Align.Stretch && vertical == Align.Stretch ->
                    Modifier.fillMaxSize()
                horizontal == Align.Stretch ->
                    Modifier.fillMaxWidth().wrapContentHeight(vertical.toComposeVerticalAlignment())
                vertical == Align.Stretch ->
                    Modifier.fillMaxHeight().wrapContentWidth(horizontal.toComposeHorizontalAlignment())
                else -> Modifier
            }

            Box(
                modifier = modifier,
                contentAlignment = alignment
            ) {
                children.forEach { it.compose() }
            }
        }
    })
    return ViewWrapper
}

actual inline fun ViewWriter.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWrapper {
    wrapNextIn(object : RViewWrapper(context), ScrollingBehaviors {
        override val horizontal: Boolean = horizontal
        override val vertical: Boolean = vertical
        override var showScrollBars: Boolean = true
        override val viewport: Reactive<Rect> = Signal(Rect(0.0, 0.0, 0.0, 0.0))
        override val content: Reactive<Rect> = Signal(Rect(0.0, 0.0, 0.0, 0.0))
        override val directlyInteractingWithScroller: Reactive<Boolean> = Signal(false)
        override var snapToElements: Pair<Align?, Align?> = null to null
        override var scrollSnapStop: Boolean = false

        override fun scrollTo(left: Double, top: Double, animated: Boolean) {
            // TODO: Implement programmatic scrolling
        }

        override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
            // TODO: Implement scroll to element
        }

        override fun scrollToKeepAnimations(x: Double, y: Double) {
            // TODO: Implement scroll to keep animations
        }

        init {
            setup()
        }

        @Composable
        override fun compose() {
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            var modifier: Modifier = Modifier
            if (this.vertical) {
                modifier = modifier.verticalScroll(verticalScrollState)
            }
            if (this.horizontal) {
                modifier = modifier.horizontalScroll(horizontalScrollState)
            }

            Box(modifier = modifier) {
                children.forEach { it.compose() }
            }
        }
    })
    return ViewWrapper
}

actual inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWrapper {
    // TODO: Implement pull-to-refresh with Compose
    // For now, just make it scrollable without refresh
    return __scrollsUncontracted(vertical, horizontal, setup)
}

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    wrapNextIn(object : RViewWrapper(context) {
        @Composable
        override fun compose() {
            var modifier: Modifier = Modifier

            // Apply width constraints
            constraints.width?.let { width ->
                modifier = modifier.width(width.value.dp)
            } ?: run {
                constraints.minWidth?.let { minWidth ->
                    modifier = modifier.widthIn(min = minWidth.value.dp)
                }
                constraints.maxWidth?.let { maxWidth ->
                    modifier = modifier.widthIn(max = maxWidth.value.dp)
                }
            }

            // Apply height constraints
            constraints.height?.let { height ->
                modifier = modifier.height(height.value.dp)
            } ?: run {
                constraints.minHeight?.let { minHeight ->
                    modifier = modifier.heightIn(min = minHeight.value.dp)
                }
                constraints.maxHeight?.let { maxHeight ->
                    modifier = modifier.heightIn(max = maxHeight.value.dp)
                }
            }

            // Apply aspect ratio if specified
            constraints.aspectRatio?.let { ratio ->
                modifier = modifier.aspectRatio(ratio.toFloat())
            }

            Box(modifier = modifier) {
                children.forEach { it.compose() }
            }
        }
    })
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper {
    // TODO: Implement reactive size constraints properly
    // For now, just no-op since we can't easily track reactive changes in Compose without more context
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.shownWhen(
    default: Boolean,
    condition: ReactiveContext.() -> Boolean
): ViewWrapper {
    wrapNextIn(object : RViewWrapper(context) {
        @Composable
        override fun compose() {
            // TODO: Properly implement reactive condition tracking
            // For now, just use the default value
            if (default) {
                children.forEach { it.compose() }
            }
        }
    })
    return ViewWrapper
}

// Helper extension functions
private fun Align.toComposeHorizontalAlignment(): Alignment.Horizontal = when (this) {
    Align.Start -> Alignment.Start
    Align.Center -> Alignment.CenterHorizontally
    Align.End -> Alignment.End
    else -> Alignment.CenterHorizontally
}

private fun Align.toComposeVerticalAlignment(): Alignment.Vertical = when (this) {
    Align.Start -> Alignment.Top
    Align.Center -> Alignment.CenterVertically
    Align.End -> Alignment.Bottom
    else -> Alignment.CenterVertically
}