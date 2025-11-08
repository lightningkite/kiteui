package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

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

var ViewWriter.overlayFrame by rContextAddon<RView?>(null)
var ViewWriter.coordinatorFrame by rContextAddon<CoordinatorFrame?>(null)

fun ViewWriter.appBase(main: PageNavigator, dialog: PageNavigator? = null, mainLayout: ContainingView.() -> Unit): ViewModifiable {
    coordinatorFrame {
        mainPageNavigator = main
        dialog?.let {
            dialogPageNavigator = it
        }
        main.bindToPlatform(context)
        pageNavigator = main
        overlayFrame = this
        coordinatorFrame = this
        mainLayout()
        dialog?.let {
            navigatorViewDialog()
        }
//        baseStack = this
//        baseStackWriter = split()
    }
}