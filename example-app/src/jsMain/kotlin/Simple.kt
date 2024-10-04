package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.ReactiveContext
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.KeyCodes
import com.lightningkite.kiteui.views.direct.swapView
import com.lightningkite.kiteui.views.direct.swapping
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.appBase
import com.lightningkite.kiteui.views.l2.navigatorView
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.events.KeyboardEvent
import org.w3c.files.BlobPropertyBag

fun main() {
    var created: RView? = null
    window.onerror = { a, b, c, d, e ->
        println("ON ERROR HANDLER $a $b $c $d $e")
        if (e is Exception) e.printStackTrace2()
    }
    val context = RContext("/")
    root(appTheme.value) {
        beforeNextElementSetup {
            ::themeChoice { ThemeDerivation(appTheme()) }
        }
        app(ScreenNavigator { AutoRoutes }, ScreenNavigator { AutoRoutes })
    }

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
