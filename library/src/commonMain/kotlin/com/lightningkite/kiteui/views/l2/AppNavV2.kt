package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

fun ViewWriter.navLayout(
    appName: String = "My App",
    appIcon: Icon = Icon.star,
    appLogo: ImageSource = Icon.star.toImageSource(Color.gray),
    navItems: List<NavElement>,
    currentUser: ReactiveContext.() -> UserInfo?,
    additionalSetup: CalculationContext.()->Unit
) {

}

fun ViewWriter.navBottomBar(show: Reactive<Boolean> = Constant(true), navElements: ReactiveContext.() -> List<NavElement>) {
    nav.row {
        ::shown { show() && !AppState.softInputOpen() }
        navGroupTabs(remember { navElements() }) {}
    } 
}

fun ViewWriter.navSideBar(navElements: ReactiveContext.() -> List<NavElement>) {

}

var ElementContext.overlayFrame by contextAddon<ContainerElement?>(null)
var ElementContext.coordinatorFrame by contextAddon<CoordinatorFrame?>(null)

@Deprecated("Use directly through context", ReplaceWith("context.overlayFrame"))
var Element.overlayFrame
    get() = context.overlayFrame
    set(value) { context.overlayFrame = value }

@Deprecated("Use directly through context", ReplaceWith("context.coordinatorFrame"))
var Element.coordinatorFrame
    get() = context.coordinatorFrame
    set(value) { context.coordinatorFrame = value }

fun ViewWriter.appBase(main: PageNavigator, dialog: PageNavigator? = null, mainLayout: ContainingView.() -> Unit) {
    coordinatorFrame {
        context.mainPageNavigator = main
        dialog?.let {
            context.dialogPageNavigator = it
        }
        main.bindToPlatform(context)
        context.pageNavigator = main
        context.overlayFrame = this
        context.coordinatorFrame = this
        mainLayout()
        dialog?.let {
            navigatorViewDialog()
        }
//        baseStack = this
//        baseStackWriter = split()
    }
}