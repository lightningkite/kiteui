package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeChoice
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
    var created: RView? = null
    window.onerror = { a, b, c, d, e ->
        println("ON ERROR HANDLER $a $b $c $d $e")
        if(e is Exception) e.printStackTrace2()
    }
    object : ViewWriter() {
        override val context: RContext = RContext("/")

        override fun addChild(view: RView) {
            document.body?.append(view.native.create())
//            created = view
        }

        val theme: suspend () -> Theme = { appTheme() }

        init {
            beforeNextElementSetup {
                useBackground = UseBackground.Yes
                ::themeChoice { ThemeChoice.Set(theme()) }
            }
        }
    }.run {
//        swapView {
//            swapping(current = { true }) {
//                text("Creatd")
//            }
//        }

        app(ScreenNavigator { AutoRoutes }, ScreenNavigator { AutoRoutes })
//        icon { source = Icon.star }

//        col {
//            themeChoice = ThemeChoice.Set(appTheme.value)
//            card - row {
//                card - text("A")
//                expanding - card - stack { text("B") }
//                card - text("C")
//            }
//            card - row {
//                space()
//                card - text("A")
//                expanding - card - stack { text("B") }
//                card - text("C")
//                space()
//            }
//        }
    }
//    document.body?.append(created!!.native.create())
//    with(context) {
//        col {
//            repeat(5) {
//                onlyWhen { true }
//                hasPopover { text("POPOVER") }
//                text("TEST")
//            }
//        }
//    }
}
