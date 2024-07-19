package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.KeyCodes
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent
import org.w3c.files.BlobPropertyBag

fun main() {
    var created: RView? = null
    window.onerror = { a, b, c, d, e ->
        println("ON ERROR HANDLER $a $b $c $d $e")
        if (e is Exception) e.printStackTrace2()
    }
    val context = RContext("/")
    object : ViewWriter() {
        override val context: RContext = context

        override fun addChild(view: RView) {
            document.body?.append(view.native.create())
//            created = view
        }

        val theme: suspend () -> Theme = { appTheme() }

        init {
            beforeNextElementSetup {
                ::themeChoice { ThemeDerivation(theme()) }
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

    document.addEventListener("keydown", { e ->
        e as KeyboardEvent
        if(e.ctrlKey && e.code == KeyCodes.letter('E')) {
            e.preventDefault()
            e.stopPropagation()
            println("Preparing export")
            launchGlobal {
                val s = context.dynamicCss.emit()
                println("Export ready, downloading")
                ExternalServices.download("static.css", Blob(arrayOf(s), BlobPropertyBag(
                    type = "text/html"
                )
                ))
            }
        }
    })
}
