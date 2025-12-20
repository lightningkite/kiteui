package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import javax.swing.JPanel
import kotlin.math.roundToInt

actual class RowCollapsingToColumn actual constructor(
    context: RContext,
    breakpoints: List<Dimension>
) : RView(context) {
    override val native = JPanel()
    private val layoutManager = LinearLayoutManager(vertical = true)
    init {
        native.layout = layoutManager

        reactiveScope {
            val w = AppState.windowInfo().width
            val index = breakpoints.indexOfFirst { w > it }

            // If index is -1 (width is less than all breakpoints) or odd index, use vertical layout
            // If index is even, use horizontal layout
            if (index == -1 || index % 2 == 1) {
                layoutManager.vertical = true
                layoutManager.ignoreWeights = true
            } else {
                layoutManager.vertical = false
                layoutManager.ignoreWeights = false
            }
            native.revalidate()
        }
    }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            layoutManager.gap = (value ?: theme.gap).value.roundToInt()
            native.revalidate()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        layoutManager.gap = (gap ?: theme.theme.gap).value.roundToInt()
    }
}
