package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement


public expect class ProgrammaticLayout(context: ElementContext): NativeContainerElement, LinearLayoutElement {
    override var gap: Dimension?
    public var delegate: ProgrammaticLayoutDelegate
    public fun invalidateLayout()

    @Deprecated("Will probably be removed in the future.")
    override val spacingForChildCornerRadii: Dimension
}

public interface ProgrammaticLayoutDelegate {
    public fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size
    public fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size)
    public object AllFull: ProgrammaticLayoutDelegate {
        override fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size {
            return within
        }
        override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            layout.children.forEach { inProgress.place(it, 0.0, 0.0, within.width, within.height) }
        }
    }
}

public interface ProgrammingLayoutInProgress {
    public val within: Size
    public val gap: Double
    public val padding: Double
    public val paddingTop: Double
    public val paddingLeft: Double
    public val paddingRight: Double
    public val paddingBottom: Double
    public fun measure(child: Element, sizeConstraint: Size): Size
    public fun place(child: Element, left: Double, top: Double, right: Double, bottom: Double)
    public fun existingPosition(child: Element): Rect
}
