package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

public fun ViewWriter.navBottomBar(show: Reactive<Boolean> = Constant(true), navElements: ReactiveContext.() -> List<NavElement>) {
    nav.row {
        ::shown { show() && !AppState.softInputOpen() }
        navGroupTabs(remember { navElements() }) {}
    }
}

public var ElementContext.overlayFrame: ContainerElement? by contextAddon<ContainerElement?>(null)
public var ElementContext.coordinatorFrame: CoordinatorFrame? by contextAddon<CoordinatorFrame?>(null)

@Deprecated("Use directly through context", ReplaceWith("context.overlayFrame"))
public var Element.overlayFrame: ContainerElement?
    get() = context.overlayFrame
    set(value) { context.overlayFrame = value }

@Deprecated("Use directly through context", ReplaceWith("context.coordinatorFrame"))
public var Element.coordinatorFrame: CoordinatorFrame?
    get() = context.coordinatorFrame
    set(value) { context.coordinatorFrame = value }

public fun ElementWriter.appBase(main: PageNavigator, mainLayout: ContainerElement.() -> Unit) {
    coordinatorFrame {
        debugName = "appBase"
        context.mainPageNavigator = main
        context.pageNavigator = main
        main.bindToPlatform(context)
        context.overlayFrame = this
        context.coordinatorFrame = this
        mainLayout()
//        baseStack = this
//        baseStackWriter = split()
    }
}