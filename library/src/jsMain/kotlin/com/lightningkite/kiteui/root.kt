package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.signal.CalculationContext
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.signal.AppScope
import com.lightningkite.signal.Readable
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi

fun root(theme: Theme, app: ViewWriter.()->Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    object : ViewWriter(), CalculationContext by AppScope {
        override val context: RContext = RContext(basePath)

        override fun addChild(view: RView) {
            document.body?.append(view.native.create())
//            created = view
        }

        init {
            beforeNextElementSetup {
                themeChoice = ThemeDerivation.SetAsBase(theme)
            }
        }
    }.also(app)
}
fun root(theme: Readable<Theme>, app: ViewWriter.()->Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    object : ViewWriter(), CalculationContext by AppScope {
        override val context: RContext = RContext(basePath)

        override fun addChild(view: RView) {
            document.body?.append(view.native.create())
//            created = view
        }

        init {
            beforeNextElementSetup {
                ::themeChoice { ThemeDerivation.SetAsBase(theme()) }
            }
        }
    }.also(app)
}