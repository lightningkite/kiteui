package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.linearMeasurePolicy
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive

public abstract class NativeLinearLayoutElement(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    override var gap: Dimension? by mutableStateOf(null)
    internal abstract val layoutVertical: Boolean

    @Composable
    protected fun gapPx(): Int = with(LocalDensity.current) { (gap ?: currentTheme.theme.gap).toDp().roundToPx() }

    @Suppress("DEPRECATION")
    @Deprecated("Will probably be removed in the future.")
    override val spacingForChildCornerRadii: Dimension
        get() = super<LinearLayoutElement>.spacingForChildCornerRadii
}

public actual class RowOrCol actual constructor(context: ElementContext) : NativeLinearLayoutElement(context) {
    public actual var vertical: Boolean by mutableStateOf(true)
    override val layoutVertical: Boolean get() = vertical

    @Deprecated("This no longer works.", level = DeprecationLevel.ERROR)
    public actual fun spacingOverrideBeforeNext(amount: Dimension) {}

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(
            content = { ShownChildren() },
            modifier = modifier,
            measurePolicy = linearMeasurePolicy(vertical = vertical, gap = gapPx(), ignoreWeights = false),
        )
    }
}

public actual class RowCollapsingToColumn actual constructor(context: ElementContext, breakpoints: List<Dimension>) : NativeLinearLayoutElement(context) {
    private var collapsed by mutableStateOf(true)
    override val layoutVertical: Boolean get() = collapsed

    init {
        reactive {
            val width = AppState.windowInfo().width
            val index = breakpoints.indexOfFirst { width > it }
            collapsed = index == -1 || index % 2 == 1
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(
            content = { ShownChildren() },
            modifier = modifier,
            measurePolicy = linearMeasurePolicy(vertical = collapsed, gap = gapPx(), ignoreWeights = collapsed),
        )
    }
}

public actual class RowWrapping actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    actual override var gap: Dimension? by mutableStateOf(null)

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val spacing = Arrangement.spacedBy((gap ?: currentTheme.theme.gap).toDp())
        FlowRow(modifier = modifier, horizontalArrangement = spacing, verticalArrangement = spacing) { ShownChildren() }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Will probably be removed in the future.")
    override val spacingForChildCornerRadii: Dimension
        get() = super<LinearLayoutElement>.spacingForChildCornerRadii
}
