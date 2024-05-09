package com.lightningkite.kiteui.views.l2

import ViewWriter
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

fun RView.navigatorView(navigator: ScreenStack) {
    val n = navigator
    this.swapView {
        var lastStack = n.stack.value
        this@swapView.swapping(
            transition = {
                val newStack = n.stack.value
                val transitionSet = theme.bodyTransitions
                when {
                    newStack.size - lastStack.size > 0 -> transitionSet.forward
                    newStack.size - lastStack.size < 0 && newStack.firstOrNull() == lastStack.firstOrNull() -> transitionSet.reverse
                    else -> transitionSet.neutral
                }.also { lastStack = newStack }
            },
            current = { n.currentScreen.await() },
            views = { screen ->
                with(split()) {
                    this.navigator = n
                    if (screen != null)
                        with(screen) { mainContent - padded; render() }
                }
            }
        )
    }
}

fun RView.navigatorViewDialog() {
    val n = navigator.dialog
    this.swapView {
        ignoreInteraction = true
        var lastStack = n.stack.value
        this@swapView.swapping(
            transition = {
                val newStack = n.stack.value
                val transitionSet = theme.dialogTransitions
                when {
                    newStack.size - lastStack.size > 0 -> transitionSet.forward
                    newStack.size - lastStack.size < 0 && newStack.firstOrNull() == lastStack.firstOrNull() -> transitionSet.reverse
                    else -> transitionSet.neutral
                }.also { lastStack = newStack }
            },
            current = { n.currentScreen.await() },
            views = { screen ->
                with(split()) {
                    this.navigator = n
                    if (screen != null)
                        with(screen) { padded; render() }
                }
            }
        )
    }
}
