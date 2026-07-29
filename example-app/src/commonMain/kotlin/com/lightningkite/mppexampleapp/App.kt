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
        appNav(navigator) {
            appName = "KiteUI Sample App"

            ::navItems {
                listOf(
                    NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                    NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
                    NavLink(title = { "Material Icons" }, icon = { Icon.search }) { { MaterialIconLibraryPage } },
                    NavLink(title = { "Shorthand Builder" }, icon = { Icon.list }) { { ShorthandBuilderPage } },
                    NavLink(title = { "Test Pages" }, icon = { Icon.home }) { { RootPage } },
                )
            }

            ::exists {
                navigator.currentPage() !is UseFullPage
            }

            actions = listOf(
                NavLink(
                    title = { "Search" },
                    icon = { Icon.search },
                    destination = { { DocSearchPage } }
                ),
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
