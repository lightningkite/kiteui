package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi

fun root(theme: Theme, app: ViewWriter.()->Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    object : ViewWriter(), CalculationContext by AppScope {
        override val context: RContext = RContext("/")

        override fun addChild(view: RView) {
            document.body?.append(view.native.create())
//            created = view
        }

        init {
            println("Root")
            beforeNextElementSetup {
                println("Set the theme")
                themeChoice = ThemeDerivation { theme.withBack }
            }
        }
    }.also(app)
}