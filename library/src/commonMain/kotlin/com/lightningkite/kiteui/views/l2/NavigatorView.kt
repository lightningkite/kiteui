package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

fun ViewWriter.navigatorView(navigator: ScreenStack) {
    val n = navigator
    this.swapView {
        var transitionSet = ScreenTransitions.Fade
        val theme = currentTheme
        reactiveScope {
            transitionSet = theme().bodyTransitions
        }
        var lastStack = n.stack.value
        this@swapView.swapping(
            transition = {
                val newStack = n.stack.value
                when {
                    newStack.size - lastStack.size > 0 -> transitionSet.forward
                    newStack.size - lastStack.size < 0 && newStack.firstOrNull() == lastStack.firstOrNull() -> transitionSet.reverse
                    else -> transitionSet.neutral
                }.also { lastStack = newStack }
            },
            current = { n.currentScreen.await() },
            views = { screen ->
                this.navigator = n
                if (screen != null)
                    with(screen) { mainContent - padded - render() }
            }
        )
    }
}

fun ViewWriter.navigatorViewDialog() {
    val n = navigator.dialog
    this.swapViewDialog {
        ignoreInteraction = true
        var transitionSet = ScreenTransitions.Fade
        val theme = currentTheme
        reactiveScope {
            transitionSet = theme().dialogTransitions
        }
        var lastStack = n.stack.value
        this@swapViewDialog.swapping(
            transition = {
                val newStack = n.stack.value
                when {
                    newStack.size - lastStack.size > 0 -> transitionSet.forward
                    newStack.size - lastStack.size < 0 && newStack.firstOrNull() == lastStack.firstOrNull() -> transitionSet.reverse
                    else -> transitionSet.neutral
                }.also { lastStack = newStack }
            },
            current = { n.currentScreen.await() },
            views = { screen ->
                this.navigator = n
                if (screen != null)
                    with(screen) { padded - render() }
            }
        )
    }
}
