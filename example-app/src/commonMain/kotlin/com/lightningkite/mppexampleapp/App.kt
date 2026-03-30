package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.ExceptionToMessage
import com.lightningkite.kiteui.isDevelopment
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.DocSearchPage
import com.lightningkite.mppexampleapp.internal.RootPage
import com.lightningkite.reactive.core.*
import kotlin.time.Duration.Companion.seconds

val defaultTheme = Theme.flat2("flat2default", 0.6.turns).customize(
    newId = "asdf",
    transitionDuration = 0.2.seconds,
    bodyTransitions = ScreenTransitions.HorizontalSlide
)
//val defaultTheme = Theme.shadCnLike("shadcnlike", background = Color.white)
val appTheme = Signal<Theme>(defaultTheme)

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
    context.exceptionHandlers += ExceptionHandler.stacktraceDialog
    context.exceptionHandlers += ExceptionToMessage.unexpectedError

    context.exceptionHandlers += ExceptionHandler<IllegalArgumentException> {
        toast("Oops")

        return@ExceptionHandler {}
    }

    Element.Debugger.leakDetect = true
    val rootView = produceExactlyOne {
        appNav(navigator, dialog) {
            appName = "KiteUI Sample App"
            ::navItems {
                listOf(
                    NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                    NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
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

    if (Platform.isDevelopment) {
        AiDriver.connect(
            appName = "example",
            rootView = { rootView },
            navigator = { navigator },
        )
    }
}

interface UseFullPage
