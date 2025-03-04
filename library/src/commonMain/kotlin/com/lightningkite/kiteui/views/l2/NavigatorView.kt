package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

fun ViewWriter.navigatorView(navigator: PageNavigator): SwapView {
    val n = navigator
    return this.swapView {
        debugName = "navigatorView"
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
            current = { n.currentPage<Page?>() },
            views = { screen ->
                with(split()) {
                    this.pageNavigator = n
                    if (screen != null)
                        with(screen) { MainContentSemantic.onNext - padded - render() }
                }
            }
        )
    }
}

fun ViewWriter.navigatorViewDialog(): SwapView {
    val n = dialogPageNavigator
    return this.swapView {
        debugName = "navigatorViewDialog"
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
            current = { n.currentPage<Page?>() },
            views = { screen ->
                with(split()) {
                    this.pageNavigator = n
                    if (screen != null)
                        with(screen) { DialogSemantic.onNext - render() }
                }
            }
        )
    }
}
