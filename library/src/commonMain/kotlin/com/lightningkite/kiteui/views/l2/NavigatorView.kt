package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.AccessibleSemantic
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.models.LiveRegionMode
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.reactive

fun ElementWriter.navigatorView(navigator: PageNavigator): SwapView {
    return swapView {
        debugName = "navigatorView"
        accessibleLiveRegion = LiveRegionMode.Polite
        Element.Debugger.debugTarget = this
        var lastStack = navigator.stack.value
        swapping(
            transition = {
                val newStack = navigator.stack.value
                val transitionSet = theme.bodyTransitions
                when {
                    newStack.size - lastStack.size > 0 -> transitionSet.forward
                    newStack.size - lastStack.size < 0 && newStack.firstOrNull() == lastStack.firstOrNull() -> transitionSet.reverse
                    else -> transitionSet.neutral
                }.also { lastStack = newStack }
            },
            current = { navigator.currentPage() },
            views = { screen ->
                with(split()) {
                    context.pageNavigator = navigator
                    if (screen != null)
                        with(screen) { themed(MainContentSemantic).beforeSetup { accessibleSemantic = AccessibleSemantic.Main }.padded.render() }
                    else null
                }
                this@swapView.requestFocusOrDescendant()
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
                    context.pageNavigator = n
                    if (screen != null)
                        with(screen) { themed(DialogSemantic).render() }
                    else null
                }
            }
        )
    }
}
