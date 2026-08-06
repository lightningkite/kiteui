package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.installDebugHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.DocSearchPage
import com.lightningkite.mppexampleapp.docs.MaterialIconLibraryPage
import com.lightningkite.mppexampleapp.docs.ShorthandBuilderPage
import com.lightningkite.mppexampleapp.internal.RootPage
import com.lightningkite.reactive.core.*
import kotlin.time.Duration.Companion.seconds

val defaultTheme = Theme.flat2("flat2-default", 0.6.turns)
//val defaultTheme = Theme.shadCnLike("shadcnlike", background = Color.white)
val appTheme = Signal<Theme>(defaultTheme)

class ToastException(override val message: String) : Exception()

fun ViewWriter.app(navigator: PageNavigator) {
    debugMode = true
//    configureTelemetry(navigator)

    context.exceptionHandlers.installDebugHandlers()

    context.exceptionHandlers += ExceptionHandler<ToastException>(1f) {
        toast(it.message)

        return@ExceptionHandler {}
    }

    context.exceptionHandlers += ExceptionHandler(0f) {
        val t = (it.cause as? ToastException) ?: return@ExceptionHandler null

        toast("Error caused by ${t.message}")

        return@ExceptionHandler {}
    }

    Element.Debugger.leakDetect = true
    val rootView = produceExactlyOneElement {
        appBase(navigator) {
            navWebStyle(
                appLogo = Icon.star.toImageSource(Color.green),
                appName = "KiteUI Example",
                showNav = { navigator.currentPage() !is UseFullPage },
                menuItems = {
                    listOf(
                        NavElement2.Link(title = "Home", icon = Icon.home) { HomePage() },
                        NavElement2.Link(title = "Documentation", icon = Icon.list) { DocSearchPage },
                        NavElement2.Link(title = "Material Icons", icon = Icon.search) { MaterialIconLibraryPage },
                        NavElement2.Link(title = "Shorthand Builder", icon = Icon.list) { ShorthandBuilderPage },
                        NavElement2.Link(title = "Test Pages", icon = Icon.home) { RootPage },
                    )
                },
                actionItems = {
                    listOf(
                        NavElement2.Link(
                            title = "Search",
                            icon = Icon.search,
                            to = { DocSearchPage }
                        ),
                    )
                }
            )
        }
    }

//    if (Platform.isDevelopment) {
//        AiDriver.connect(
//            appName = "example",
//            rootView = { rootView },
//            navigator = { navigator },
//        )
//    }
}

interface UseFullPage
