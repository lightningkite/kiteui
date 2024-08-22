package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

fun ViewWriter.navLayout(
    appName: String = "My App",
    appIcon: Icon = Icon.star,
    appLogo: ImageSource = Icon.star.toImageSource(Color.gray),
    navItems: List<NavElement>,
    currentUser: ReactiveContext.() -> UserInfo?,
    additionalSetup: CalculationContext.()->Unit
) {

}

fun ViewWriter.navBottomBar(show: Readable<Boolean> = Constant(true), navElements: ReactiveContext.() -> List<NavElement>) {
    row {
        ::exists { show() && !AppState.softInputOpen() }
        navGroupTabs(shared { navElements() }) {}
    } 
}

fun ViewWriter.navSideBar(navElements: ReactiveContext.() -> List<NavElement>) {

}

var ViewWriter.overlayStack by rContextAddon<Stack?>(null)

fun ViewWriter.appBase(main: ScreenNavigator, dialog: ScreenNavigator? = null, mainLayout: ContainingView.() -> Unit) {
    stack {
        spacing = 0.px
        mainScreenNavigator = main
        dialog?.let {
            dialogScreenNavigator = it
        }
        main.bindToPlatform(context)
        screenNavigator = main
        overlayStack = this
        mainLayout()
        dialog?.let {
            navigatorViewDialog() in DialogSemantic.onNext
        }
//        baseStack = this
//        baseStackWriter = split()
    }
}