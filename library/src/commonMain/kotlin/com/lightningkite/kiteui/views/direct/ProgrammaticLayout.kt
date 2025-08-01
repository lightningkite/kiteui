package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.signal.Listenable
import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView


public expect class ProgrammaticLayout(context: RContext): RView {
    public var delegate: ProgrammaticLayoutDelegate
    public fun invalidateLayout()
}

public interface ProgrammaticLayoutDelegate {
    public fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size
    public fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size)
    public object AllFull: ProgrammaticLayoutDelegate {
        public override fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size {
            return within
        }
        public override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            layout.children.forEach { inProgress.place(it, 0.0, 0.0, within.width, within.height) }
        }
    }
}

public interface ProgrammingLayoutInProgress {
    public val gap: Double
    public val padding: Double
    public val paddingTop: Double
    public val paddingLeft: Double
    public val paddingRight: Double
    public val paddingBottom: Double
    public fun measure(child: RView, sizeConstraint: Size): Size
    public fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double)
    public fun existingPosition(child: RView): Rect
}
