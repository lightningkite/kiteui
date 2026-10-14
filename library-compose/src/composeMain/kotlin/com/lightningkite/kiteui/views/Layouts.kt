package com.lightningkite.kiteui.views

import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import com.lightningkite.kiteui.models.Align

// KiteUI's layout rules, as the web and Android backends implement them:
// - Children stretch on an axis unless aligned otherwise.  "Stretch" means the container's size on that axis,
//   which for a container that wraps its content is its largest child.
// - Linear layouts wrap on the main axis; weight shares out the remaining space, when there is a fixed amount.
// Compose's Row/Column/Box differ (fillMaxWidth inside a wrapping parent fills the grandparent), hence these.
// Compose measures each child only once, so a wrapping container works out the size of a stretch axis from
// the children's intrinsic sizes, asked at the size each child will actually get on the other axis.

private val IntrinsicMeasurable.element: NativeElement get() = (parentData as LayoutIdParentData).layoutId as NativeElement

private fun Align?.stretches() = this == null || this == Align.Stretch

private fun Align?.offset(free: Int): Int = when (this) {
    Align.Center -> free / 2
    Align.End -> free
    else -> 0
}

private const val Infinity = Constraints.Infinity

/** Children overlap; each is stretched or aligned within the frame. */
internal val FrameMeasurePolicy: MeasurePolicy = object : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val stretchesH = measurables.map { it.element.horizontalAlign.stretches() }
        val stretchesV = measurables.map { it.element.verticalAlign.stretches() }
        val placeables = arrayOfNulls<Placeable>(measurables.size)

        // Children that stretch on neither axis don't depend on the frame's size.
        measurables.forEachIndexed { i, m ->
            if (!stretchesH[i] && !stretchesV[i]) placeables[i] = m.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        fun naturalWidth(i: Int) = placeables[i]?.width ?: measurables[i].maxIntrinsicWidth(Infinity)
        val width = if (constraints.hasFixedWidth) constraints.maxWidth
        else (measurables.indices.maxOfOrNull(::naturalWidth) ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = if (constraints.hasFixedHeight) constraints.maxHeight
        else (measurables.indices.maxOfOrNull { i ->
            placeables[i]?.height ?: measurables[i].maxIntrinsicHeight(if (stretchesH[i]) width else naturalWidth(i).coerceAtMost(width))
        } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)

        measurables.forEachIndexed { i, m ->
            if (placeables[i] == null) placeables[i] = m.measure(
                Constraints(
                    minWidth = if (stretchesH[i]) width else 0, maxWidth = width,
                    minHeight = if (stretchesV[i]) height else 0, maxHeight = height,
                )
            )
        }

        return layout(width, height) {
            placeables.forEachIndexed { i, p ->
                val e = measurables[i].element
                p!!.place(e.horizontalAlign.offset(width - p.width), e.verticalAlign.offset(height - p.height))
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        maxIntrinsicWidth(measurables, height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        maxIntrinsicHeight(measurables, width)
}

/** A row or column.  [gap] is in pixels and goes between children. */
internal fun linearMeasurePolicy(vertical: Boolean, gap: Int, ignoreWeights: Boolean): MeasurePolicy = object : MeasurePolicy {
    // Written in main/cross terms; only these helpers know the orientation.
    fun IntrinsicMeasurable.weight() = if (ignoreWeights) 0f else element.weight ?: 0f
    fun IntrinsicMeasurable.crossStretches() = (if (vertical) element.horizontalAlign else element.verticalAlign).stretches()
    fun IntrinsicMeasurable.crossAlign() = if (vertical) element.horizontalAlign else element.verticalAlign
    fun IntrinsicMeasurable.naturalMain(cross: Int) = if (vertical) maxIntrinsicHeight(cross) else maxIntrinsicWidth(cross)
    fun IntrinsicMeasurable.naturalCross(main: Int) = if (vertical) maxIntrinsicWidth(main) else maxIntrinsicHeight(main)
    fun Placeable.main() = if (vertical) height else width
    fun Placeable.cross() = if (vertical) width else height
    fun constraints(mainMin: Int, mainMax: Int, crossMin: Int, crossMax: Int) =
        if (vertical) Constraints(crossMin, crossMax, mainMin, mainMax) else Constraints(mainMin, mainMax, crossMin, crossMax)
    fun totalGap(count: Int) = gap * (count - 1).coerceAtLeast(0)

    /**
     * Each child's main-axis size: its [natural] size, or, when there's a fixed amount of [available] space,
     * a weighted child's share of what the others leave.  Shared out in order, so rounding lands on the last.
     */
    fun mainSizes(children: List<IntrinsicMeasurable>, available: Int, natural: (Int) -> Int): IntArray {
        fun weighted(i: Int) = available != Infinity && children[i].weight() > 0f
        val sizes = IntArray(children.size) { i -> if (weighted(i)) 0 else natural(i) }
        var remaining = if (available == Infinity) 0 else (available - sizes.sum() - totalGap(children.size)).coerceAtLeast(0)
        var weightLeft = children.indices.filter(::weighted).sumOf { children[it].weight().toDouble() }.toFloat()
        for (i in children.indices) {
            if (!weighted(i)) continue
            val w = children[i].weight()
            sizes[i] = (remaining * (w / weightLeft)).toInt()
            remaining -= sizes[i]
            weightLeft -= w
        }
        return sizes
    }

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val mainMin = if (vertical) constraints.minHeight else constraints.minWidth
        val mainMax = if (vertical) constraints.maxHeight else constraints.maxWidth
        val mainFixed = if (vertical) constraints.hasFixedHeight else constraints.hasFixedWidth
        val crossMin = if (vertical) constraints.minWidth else constraints.minHeight
        val crossMax = if (vertical) constraints.maxWidth else constraints.maxHeight
        val crossFixed = if (vertical) constraints.hasFixedWidth else constraints.hasFixedHeight
        val available = if (mainFixed) mainMax else Infinity
        fun weighted(m: Measurable) = mainFixed && m.weight() > 0f
        val placeables = arrayOfNulls<Placeable>(measurables.size)

        // Unweighted children can be measured now, unless they stretch to a cross size we don't know yet.
        measurables.forEachIndexed { i, m ->
            if (weighted(m) || (m.crossStretches() && !crossFixed)) return@forEachIndexed
            placeables[i] = m.measure(constraints(0, mainMax, if (m.crossStretches()) crossMax else 0, crossMax))
        }
        val main = mainSizes(measurables, available) { i ->
            placeables[i]?.main() ?: measurables[i].naturalMain(Infinity).coerceAtMost(mainMax)
        }
        val cross = if (crossFixed) crossMax
        else (measurables.indices.maxOfOrNull { i -> placeables[i]?.cross() ?: measurables[i].naturalCross(main[i]) } ?: 0)
            .coerceIn(crossMin, crossMax)
        measurables.forEachIndexed { i, m ->
            if (placeables[i] != null) return@forEachIndexed
            val (lo, hi) = if (weighted(m)) main[i] to main[i] else 0 to mainMax
            placeables[i] = m.measure(constraints(lo, hi, if (m.crossStretches()) cross else 0, cross))
        }

        val measured = placeables.map { it!! }
        val finalMain = if (mainFixed) mainMax else (measured.sumOf { it.main() } + totalGap(measured.size)).coerceIn(mainMin, mainMax)
        return layout(if (vertical) cross else finalMain, if (vertical) finalMain else cross) {
            var position = 0
            measured.forEachIndexed { i, p ->
                val crossOffset = measurables[i].crossAlign().offset(cross - p.cross())
                if (vertical) p.place(crossOffset, position) else p.place(position, crossOffset)
                position += p.main() + gap
            }
        }
    }

    fun intrinsicMain(children: List<IntrinsicMeasurable>, cross: Int) =
        children.sumOf { it.naturalMain(cross) } + totalGap(children.size)

    fun intrinsicCross(children: List<IntrinsicMeasurable>, main: Int): Int {
        val sizes = mainSizes(children, main) { i -> children[i].naturalMain(Infinity).coerceAtMost(main) }
        return children.indices.maxOfOrNull { children[it].naturalCross(sizes[it]) } ?: 0
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        if (vertical) intrinsicCross(measurables, height) else intrinsicMain(measurables, height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        if (vertical) intrinsicMain(measurables, width) else intrinsicCross(measurables, width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        maxIntrinsicWidth(measurables, height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        maxIntrinsicHeight(measurables, width)
}
