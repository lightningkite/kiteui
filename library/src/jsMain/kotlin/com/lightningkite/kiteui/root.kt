package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi

fun root(theme: Theme, app: ViewWriter.()->Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    object : ViewWriter(), CalculationContext by AppScope {
        override val context: RContext = RContext(basePath)

        override fun addChild(view: RView) {
            view.themeChoice = ThemeDerivation.SetAsBase(theme)
            document.body?.append(view.native.create())
//            created = view
        }
    }.also(app)
}
fun root(theme: Reactive<Theme>, app: ViewWriter.()->Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    object : ViewWriter(), CalculationContext by AppScope {
        override val context: RContext = RContext(basePath).also {
            ExternalServices.baseContext = it
        }

        override fun addChild(view: RView) {
            with(view) {
                ::themeChoice { ThemeDerivation.SetAsBase(theme()) }
            }
            document.body?.append(view.native.create())
//            created = view
        }
    }.apply {
        if(debugMode) {
            val safe = Signal(Edges.ZERO)
            safeInsets = safe
            var times = 0
            AppState.onUniversalKeyboard {
                if(it.alt && it.code == KeyCodes.letter('e')) {
                    println("Setting edges")
                    safe.value = if(times++ % 2 == 0) Edges(100.dp) else Edges.ZERO
                    true
                } else false
            }
        }
    }.also(app)
}