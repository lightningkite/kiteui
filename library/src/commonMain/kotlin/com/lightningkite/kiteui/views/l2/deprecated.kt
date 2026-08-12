package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.CoordinatorFrame
import com.lightningkite.kiteui.views.direct.IconView
import com.lightningkite.kiteui.views.direct.icon as correctIcon
import com.lightningkite.reactive.core.Signal

@Deprecated("Import has moved", ReplaceWith("this.icon(icon, description)", "com.lightningkite.kiteui.views.direct.icon"))
public fun ElementWriter.icon(icon: Icon, description: String): IconView = correctIcon(icon, description)

@Deprecated("Use directly through context", ReplaceWith("context.appNavFactory"))
public val ElementWriter.appNavFactory: Signal<ViewWriter.(AppNav.() -> Unit) -> Unit>
    get() = context.appNavFactory

@Deprecated("Use directly through context", ReplaceWith("context.overlayFrame"))
public var Element.overlayFrame: ContainerElement
    get() = context.overlayFrame
    set(value) { context.overlayFrame = value }

@Deprecated("Use directly through context", ReplaceWith("context.coordinatorFrame"))
public var Element.coordinatorFrame: CoordinatorFrame
    get() = context.coordinatorFrame
    set(value) { context.coordinatorFrame = value }