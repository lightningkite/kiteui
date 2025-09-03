package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.rowWrapping
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.textInput
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

//val defaultTheme = Theme.flat2("default", Angle(0.55f)).customize(
//    "defaulter",
//    transitionDuration = 150.milliseconds,
//    bodyTransitions = ScreenTransitions.HorizontalSlide,
//    derivations = mapOf(
//        EmphasizedSemantic to {
//            it.copy(
//                id = EmphasizedSemantic.key,
//                font = it.font.copy(bold = true, italic = true),
//                foreground = Color.orange
//            ).withoutBack
//        }
//    ))
//val appTheme = Signal<Theme>(defaultTheme)

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

    return col {
        val signalTest = Signal("test")
        val alignSignal = Signal(Align.Start)
        text("Hello World")
        textInput {
            content bind signalTest
        }
        text {
            ::content {
                signalTest()
            }
            ::align {
                alignSignal()
            }
        }
    }

//    return appNav(navigator, dialog) {
//        appName = "KiteUI Sample App"
//        ::navItems {
//            listOf(
////                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
////                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
//                NavLink(title = { "Test Pages" }, icon = { Icon.home }) { { TestPage } },
//            )
//        }
//
//        ::exists {
//            navigator.currentPage() !is UseFullPage
//        }
//
//    }
}

interface UseFullPage
