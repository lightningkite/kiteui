package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlin.math.roundToInt

// The delegate works in dp (the web's CSS px) and may measure any child, any number of times, including
// children it adds during the pass (Recycler2 does).  So children are subcomposed on demand, measured
// through their intrinsics, and only truly measured once, at the size the delegate places them.
public actual class ProgrammaticLayout actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    actual override var gap: Dimension? by mutableStateOf(null)
    public actual var delegate: ProgrammaticLayoutDelegate by mutableStateOf(ProgrammaticLayoutDelegate.AllFull)

    // Read while measuring, so changing it measures again.
    private var layoutRequests by mutableIntStateOf(0)
    private var measuring = false

    public actual fun invalidateLayout() {
        // As on Android, requests made by the delegate during its own pass are ignored.
        if (measuring) return
        // `++` reads the counter, and a read during another layout's measure (Recycler2 invalidates its
        // scroll indicator while measuring its cells) would make that layout measure again, forever.
        Snapshot.withoutReadObservation { layoutRequests++ }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Will probably be removed in the future.")
    actual override val spacingForChildCornerRadii: Dimension
        get() = super<LinearLayoutElement>.spacingForChildCornerRadii

    // The delegate offsets children by the padding itself, so Render must not apply it too.
    private var padding by mutableStateOf(Edges.ZERO)
    override fun refreshPadding() {
        padding = appliedPadding
    }

    // Children keep their last position until the delegate places them again, as Android views do.
    private val placed = HashMap<NativeElement, Rect>()

    // subcompose recomposes a child whenever it is given a new content lambda, so each child keeps one.
    private val slots = HashMap<NativeElement, @Composable () -> Unit>()

    @Composable
    override fun Content(modifier: Modifier) {
        SubcomposeLayout(modifier.then(NoIntrinsicSize)) { constraints ->
            layoutRequests
            fun Double.toPx(): Int = if (isInfinite()) Constraints.Infinity else (this * density).roundToInt()
            fun Int.toDp(): Double = if (this == Constraints.Infinity) Double.POSITIVE_INFINITY else this / density.toDouble()

            // Every shown child is subcomposed each pass, or Compose disposes it.
            val measurables = HashMap<NativeElement, Measurable>()
            fun measurable(child: Element): Measurable? {
                val e = child.underlyingNativeElement
                if (!e.takesSpace) return null
                return measurables.getOrPut(e) { subcompose(e, slots.getOrPut(e) { { e.Render() } }).single() }
            }
            nativeChildren.toList().forEach(::measurable)
            val current = nativeChildren.toSet()
            placed.keys.retainAll(current)
            slots.keys.retainAll(current)

            val inProgress = object : ProgrammingLayoutInProgress {
                override var within = Size(constraints.maxWidth.toDp(), constraints.maxHeight.toDp())
                override val gap: Double = (this@ProgrammaticLayout.gap ?: currentTheme.theme.gap).px
                override val padding: Double get() = paddingLeft
                override val paddingTop: Double = this@ProgrammaticLayout.padding.top.px
                override val paddingLeft: Double = this@ProgrammaticLayout.padding.left.px
                override val paddingRight: Double = this@ProgrammaticLayout.padding.right.px
                override val paddingBottom: Double = this@ProgrammaticLayout.padding.bottom.px

                override fun measure(child: Element, sizeConstraint: Size): Size {
                    // A hidden child takes no space, like display: none on the web.
                    val m = measurable(child) ?: return Size.Zero
                    val maxHeight = sizeConstraint.height.toPx()
                    val width = m.maxIntrinsicWidth(maxHeight).coerceAtMost(sizeConstraint.width.toPx())
                    val height = m.maxIntrinsicHeight(width).coerceAtMost(maxHeight)
                    return Size(width.toDp(), height.toDp())
                }

                override fun place(child: Element, left: Double, top: Double, right: Double, bottom: Double) {
                    placed[child.underlyingNativeElement] = Rect(left, top, right, bottom)
                }

                override fun existingPosition(child: Element): Rect = placed[child.underlyingNativeElement] ?: Rect.Zero
            }

            measuring = true
            val width: Int
            val height: Int
            try {
                val natural = delegate.measure(this@ProgrammaticLayout, inProgress, inProgress.within)
                // Delegates that fill their space (AllFull, or Recycler2 before it knows its viewport) answer with
                // `within`, which is unbounded inside a scroll.  That can't be laid out, so it takes the least allowed.
                fun Double.orLeast(least: Int) = if (isInfinite()) least else toPx()
                width = natural.width.orLeast(constraints.minWidth).coerceIn(constraints.minWidth, constraints.maxWidth)
                height = natural.height.orLeast(constraints.minHeight).coerceIn(constraints.minHeight, constraints.maxHeight)
                inProgress.within = Size(width.toDp(), height.toDp())
                delegate.layout(this@ProgrammaticLayout, inProgress, inProgress.within)
            } finally {
                measuring = false
            }

            val placeables = placed.mapNotNull { (child, rect) ->
                val m = measurables[child] ?: return@mapNotNull null
                val left = rect.left.toPx()
                val top = rect.top.toPx()
                val size = Constraints.fixed((rect.right.toPx() - left).coerceAtLeast(0), (rect.bottom.toPx() - top).coerceAtLeast(0))
                Triple(m.measure(size), left, top)
            }
            layout(width, height) {
                placeables.forEach { (p, x, y) -> p.place(x, y) }
            }
        }
    }
}

// SubcomposeLayout can't answer intrinsic queries, and a programmatic layout has no size of its own apart from
// the space it's given, so it reports none, like an empty box.
private object NoIntrinsicSize : LayoutModifier {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        return layout(p.width, p.height) { p.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = 0
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = 0
}
