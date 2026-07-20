package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.math.min

@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atStart get() = align(Align.Start, Align.Stretch)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atEnd get() = align(Align.End, Align.Stretch)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atTop get() = align(Align.Stretch, Align.Start)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atBottom get() = align(Align.Stretch, Align.End)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.centeredHorizontally get() = align(Align.Center, Align.Stretch)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.centeredVertically get() = align(Align.Stretch, Align.Center)

@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atTopStart get() = align(Align.Start, Align.Start)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atCenterStart get() = align(Align.Start, Align.Center)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atBottomStart get() = align(Align.Start, Align.End)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atTopCenter get() = align(Align.Center, Align.Start)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.centered get() = align(Align.Center, Align.Center)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atBottomCenter get() = align(Align.Center, Align.End)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atTopEnd get() = align(Align.End, Align.Start)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atCenterEnd get() = align(Align.End, Align.Center)
@ViewModifierDsl3
public val ElementWriter.CanAddAlignment.atBottomEnd get() = align(Align.End, Align.End)

@ViewModifierDsl3
public inline val ElementWriter.CanAddWeight.expanding get() = weight(1f)

@ViewModifierDsl3
public fun ElementWriter.CanAddSizing.setHeight(height: Dimension) = sizedBox(SizeConstraints(height = height))
@ViewModifierDsl3
public fun ElementWriter.CanAddSizing.setWidth(height: Dimension) = sizedBox(SizeConstraints(width = height))
@ViewModifierDsl3
public fun ElementWriter.CanAddSizing.maxHeight(height: Dimension) = sizedBox(SizeConstraints(maxHeight = height))
@ViewModifierDsl3
public fun ElementWriter.CanAddSizing.maxWidth(width: Dimension) = sizedBox(SizeConstraints(maxWidth = width))

@ViewModifierDsl3
public fun ElementWriter.CanAddAlignment.maxWidthCentered(width: Dimension) =
    align(Align.Center, Align.Stretch).sizedBox(SizeConstraints(maxWidth = width))

@ViewDsl
public fun ElementWriter.icon(source: ReactiveContext.() -> Icon, description: String, setup: IconView.() -> Unit = {}) {
    icon {
        ::source { source() }
        this.description = description
        setup(this)
    }
}

public val Icon.Companion.empty get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())
