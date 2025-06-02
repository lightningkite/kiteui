package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.KeyCodes
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.docs.DocSearchPage
import com.lightningkite.mppexampleapp.internal.RootPage
import kotlin.time.Duration.Companion.milliseconds

val defaultTheme = Theme.flat2("default", Angle(0.55f)).customize(
    "defaulter",
    transitionDuration = 150.milliseconds,
    bodyTransitions = ScreenTransitions.HorizontalSlide,
    derivations = mapOf(
        EmphasizedSemantic to {
            it.copy(
                id = EmphasizedSemantic.key,
                font = it.font.copy(bold = true, italic = true),
                foreground = Color.orange
            ).withoutBack
        }
    ))
val appTheme = Property<Theme>(defaultTheme)

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator): ViewModifiable {
    RViewHelper.leakDetection = true
//    return frame {
//        this.forcedSafeInsets = Edges(100.dp)
//        col {
//            text("A")
//            text("B")
//            text("C")
//        }
//    }
    return appBase(navigator, dialog) {
        appNavFactory.value(this, {
            appName = "KiteUI Sample App"
            ::navItems {
                listOf(
                    NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                    NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootPage } },
                    NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
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
        })
        var times = 0
        onRemove(AppState.onUniversalKeyboard {
            if(it.alt && it.code == KeyCodes.letter('e')) {
                this.forcedSafeInsets = if(times++ % 2 == 0) Edges(100.dp) else Edges.ZERO
                fun apply(view: RView) {
                    view.refreshPadding()
                    for(child in view.children) apply(child)
                }
                apply(this)
                true
            } else false
        })
    }
}

interface UseFullPage
