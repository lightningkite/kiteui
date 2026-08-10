package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.math.min

public val ElementWriter.CanAddAlignment.atStart: ElementWriter.CanAddWeight get() = align(Align.Start, Align.Stretch)
public val ElementWriter.CanAddAlignment.atEnd: ElementWriter.CanAddWeight get() = align(Align.End, Align.Stretch)
public val ElementWriter.CanAddAlignment.atTop: ElementWriter.CanAddWeight get() = align(Align.Stretch, Align.Start)
public val ElementWriter.CanAddAlignment.atBottom: ElementWriter.CanAddWeight get() = align(Align.Stretch, Align.End)
public val ElementWriter.CanAddAlignment.centeredHorizontally: ElementWriter.CanAddWeight get() = align(Align.Center, Align.Stretch)
public val ElementWriter.CanAddAlignment.centeredVertically: ElementWriter.CanAddWeight get() = align(Align.Stretch, Align.Center)

public val ElementWriter.CanAddAlignment.atTopStart: ElementWriter.CanAddWeight get() = align(Align.Start, Align.Start)
public val ElementWriter.CanAddAlignment.atCenterStart: ElementWriter.CanAddWeight get() = align(Align.Start, Align.Center)
public val ElementWriter.CanAddAlignment.atBottomStart: ElementWriter.CanAddWeight get() = align(Align.Start, Align.End)
public val ElementWriter.CanAddAlignment.atTopCenter: ElementWriter.CanAddWeight get() = align(Align.Center, Align.Start)
public val ElementWriter.CanAddAlignment.centered: ElementWriter.CanAddWeight get() = align(Align.Center, Align.Center)
public val ElementWriter.CanAddAlignment.atBottomCenter: ElementWriter.CanAddWeight get() = align(Align.Center, Align.End)
public val ElementWriter.CanAddAlignment.atTopEnd: ElementWriter.CanAddWeight get() = align(Align.End, Align.Start)
public val ElementWriter.CanAddAlignment.atCenterEnd: ElementWriter.CanAddWeight get() = align(Align.End, Align.Center)
public val ElementWriter.CanAddAlignment.atBottomEnd: ElementWriter.CanAddWeight get() = align(Align.End, Align.End)

public inline val ElementWriter.CanAddWeight.expanding: ElementWriter.CanAddListElementModifier get() = weight(1f)

public fun ElementWriter.CanAddSizing.setHeight(height: Dimension): ElementWriter.CanAddTheme = sizedBox(SizeConstraints(height = height))
public fun ElementWriter.CanAddSizing.setWidth(height: Dimension): ElementWriter.CanAddTheme = sizedBox(SizeConstraints(width = height))
public fun ElementWriter.CanAddSizing.maxHeight(height: Dimension): ElementWriter.CanAddTheme = sizedBox(SizeConstraints(maxHeight = height))
public fun ElementWriter.CanAddSizing.maxWidth(width: Dimension): ElementWriter.CanAddTheme = sizedBox(SizeConstraints(maxWidth = width))

public fun ElementWriter.CanAddAlignment.maxWidthCentered(width: Dimension): ElementWriter.CanAddTheme =
    align(Align.Center, Align.Stretch).sizedBox(SizeConstraints(maxWidth = width))

public fun ElementWriter.icon(source: ReactiveContext.() -> Icon, description: String, setup: IconView.() -> Unit = {}) {
    icon {
        ::source { source() }
        this.description = description
        setup(this)
    }
}

public val Icon.Companion.empty: Icon get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())
