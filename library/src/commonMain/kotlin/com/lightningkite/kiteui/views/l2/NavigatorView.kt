package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.models.LiveRegionMode
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.kiteui.telemetry.ScreenLoad
import com.lightningkite.kiteui.telemetry.Telemetry
import com.lightningkite.kiteui.telemetry.screenLoad
import com.lightningkite.kiteui.telemetry.telemetryAttributes
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.kiteui.telemetry.TelemetryAttributesElement
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

public fun ElementWriter.navigatorView(navigator: PageNavigator): SwapView {
    return swapView {
        debugName = "navigatorView"
        Element.Debugger.debugTarget = this
        var lastStack = navigator.stack.value
        var visit: Job? = null
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
                    visit?.cancel()
                    if (screen != null) {
                        val load = ScreenLoad()
                        context.screenLoad = load
                        context.telemetryAttributes += telemetryAttributesOf(
                            TelemetryKeys.App.screenName to (screen::class.simpleName ?: "Unknown"),
                            TelemetryKeys.App.screenId to Telemetry.spanId(),
                        )
                        val telemetry = Telemetry.installed
                        if (telemetry != null) visit = this@swapView.launch(TelemetryAttributesElement(context.telemetryAttributes)) {
                            telemetry.recordScreenVisit(navigator, load)
                        }
                        with(screen) { themed(MainContentSemantic).asMain.padded.render() }
                        load.finishedRendering()
                    }
                    // Tell assistive technology the screen changed, which it can't otherwise
                    // notice. Deliberately not requestFocus: keyboard focus landing on the new
                    // page's first field pops the soft keyboard on Android and iOS. Pages that
                    // want a focused field ask for it themselves.
                    if (screen != null) launch {
                        this@swapView.announceAsNewScreen(screen.title.awaitOnce())
                    }
                }
            }
        )
    }
}
