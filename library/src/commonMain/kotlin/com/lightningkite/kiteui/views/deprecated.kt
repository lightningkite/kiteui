package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.ReactiveScopeData
import com.lightningkite.kiteui.launch as otherLaunch

@Deprecated("")
fun RView.themeModifier(calculate: (()->Theme)->Theme): ViewWrapper {
    beforeNextElementSetup {
        themeChoice = ThemeChoice.Derive { calculate { it } }
    }
    return ViewWrapper
}


@Deprecated("Wrong import; this has moved", ReplaceWith("launch", ""))
inline fun RView.launch(noinline action: suspend () -> Unit) = otherLaunch(action)
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope(action = action)", "com.lightningkite.kiteui.reactive.reactiveScope"))
inline fun RView.reactiveScope(noinline action: suspend () -> Unit) { ReactiveScopeData(this, action, null) }
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope(onLoad, action)", "com.lightningkite.kiteui.reactive.reactiveScope"))
inline fun RView.reactiveScope(noinline onLoad: (() -> Unit)?, noinline action: suspend () -> Unit) { ReactiveScopeData(this, action, onLoad) }
