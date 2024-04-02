package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactiveScope
import org.w3c.dom.asList


fun ViewWriter.handleTheme(
    view: NView,
    viewDraws: Boolean = true,
    isControl: Boolean = false,
) {
    val transition = transitionNextView
    transitionNextView = ViewWriter.TransitionNextView.No
    val currentTheme = currentTheme
    val isRoot = isRoot
    this.isRoot = false

    view.calculationContext.reactiveScope {
        val theme = currentTheme()

        val mightTransition = isControl || transition != ViewWriter.TransitionNextView.No
        val shouldTransition = when (transition) {
            ViewWriter.TransitionNextView.No -> false
            ViewWriter.TransitionNextView.Yes -> true
            is ViewWriter.TransitionNextView.Maybe -> transition.logic()
        }

        val virtualClasses = view.js.classList.asList().map { it.toString() }.toMutableSet()
        if (viewDraws) virtualClasses.add("viewDraws")
        if (isRoot) virtualClasses.add("isRoot")

        if (shouldTransition) virtualClasses.add("transition")
        else virtualClasses.remove("transition")
        if (mightTransition) virtualClasses.add("mightTransition")
        else virtualClasses.remove("mightTransition")

        virtualClasses.removeAll { it.startsWith("theme-") }
        virtualClasses.add(DynamicCSS.themeInteractive(theme))

        view.js.className = virtualClasses.joinToString(" ")
    }
}
