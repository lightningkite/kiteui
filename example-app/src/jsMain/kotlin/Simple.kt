package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.readable.Property
import com.lightningkite.readable.ReactiveContext
import com.lightningkite.readable.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.swapView
import com.lightningkite.kiteui.views.direct.swapping
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.appBase
import com.lightningkite.kiteui.views.l2.navigatorView
import com.lightningkite.readable.AppScope
import com.lightningkite.readable.AppState
import com.lightningkite.readable.onRemove
import com.lightningkite.readable.reactive
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

    root(appTheme) {
        app(PageNavigator { AutoRoutes }, PageNavigator { AutoRoutes })

        document.addEventListener("keydown", { e ->
            e as KeyboardEvent
            if(e.ctrlKey && e.code == KeyCodes.letter('E')) {
                e.preventDefault()
                e.stopPropagation()
                println("Preparing export")
                AppScope.launch {
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

}
