package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

fun ViewWriter.navigatorView(navigator: ScreenNavigator) {
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
            current = { n.currentScreen<Screen?>() },
            views = { screen ->
                with(split()) {
                    this.screenNavigator = n
                    if (screen != null)
                        with(screen) { mainContent - padded; render() }
                }
            }
        )
    }
}

fun ViewWriter.navigatorViewDialog() {
    val n = dialogScreenNavigator
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
            current = { n.currentScreen<Screen?>() },
            views = { screen ->
                with(split()) {
                    this.screenNavigator = n
                    if (screen != null)
                        with(screen) { padded; render() }
                }
            }
        )
    }
}
