package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.signal.CalculationContext
import com.lightningkite.signal.ReactiveContext
//import com.lightningkite.signal.ReactiveScopeData
//import com.lightningkite.kiteui.launch as otherLaunch

//@Deprecated("")
//fun RView.themeModifier(calculate: (()->Theme)->Theme): ViewWrapper {
//    beforeNextElementSetup {
//        themeChoice = ThemeChoice.Derive { calculate { it } }
//    }
//    return ViewWrapper
//}


////@Deprecated("Wrong import; this has moved", ReplaceWith("launch", ""))
//inline fun RView.launch(noinline action: suspend () -> Unit) = otherLaunch(action)
////@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope(action = action)", "com.lightningkite.readable.reactiveScope"))
//@OptIn(InternalKiteUi::class)
//inline fun RView.reactiveScope(noinline action: ReactiveContext.() -> Unit) { DirectReactiveContext(this, action = action, onLoad = null).run() }
////@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope(onLoad, action)", "com.lightningkite.readable.reactiveScope"))
//@OptIn(InternalKiteUi::class)
//inline fun RView.reactiveScope(noinline onLoad: (() -> Unit)?, noinline action: ReactiveContext.() -> Unit) { DirectReactiveContext(this, action = action, onLoad = onLoad).run() }



@Deprecated("Wrong import; this has moved", ReplaceWith("launch", "com.lightningkite.kiteui.launch")) val launch = Unit
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope", "com.lightningkite.readable.reactiveScope")) val reactiveScope = Unit