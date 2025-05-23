package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.signal.Listenable
import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView


expect class ProgrammaticLayout(context: RContext): RView {
    var delegate: ProgrammaticLayoutDelegate
    fun invalidateLayout()
}

interface ProgrammaticLayoutDelegate {
    fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size
    fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size)
    object AllFull: ProgrammaticLayoutDelegate {
        override fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size {
            return within
        }
        override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            layout.children.forEach { inProgress.place(it, 0.0, 0.0, within.width, within.height) }
        }
    }
}

interface ProgrammingLayoutInProgress {
    val gap: Double
    val padding: Double
    val paddingTop: Double
    val paddingLeft: Double
    val paddingRight: Double
    val paddingBottom: Double
    fun measure(child: RView, sizeConstraint: Size): Size
    fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double)
    fun existingPosition(child: RView): Rect
}
