package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.contextAddon
import com.lightningkite.services.telemetry.TelemetryAttributes
import kotlinx.coroutines.CompletableDeferred

/**
 * Ambient telemetry attributes for everything rendered in this context: every coroutine an element launches
 * carries them, so spans and metrics recorded there do too. [com.lightningkite.kiteui.views.l2.navigatorView]
 * adds the screen's name and visit id; set app-wide attributes on the root context.
 */
public var ElementContext.telemetryAttributes: TelemetryAttributes by contextAddon(TelemetryAttributes.empty)

internal var ElementContext.screenLoad: ScreenLoad? by contextAddon(null)

/**
 * A screen is usable once it has rendered and none of its elements is loading. Elements report through
 * [startedLoading]/[finishedLoading] when their background processes become not-ready and settle again.
 */
internal class ScreenLoad {
    private var loadingElements = 0
    private var rendered = false
    val usable = CompletableDeferred<Unit>()

    fun startedLoading() { loadingElements++ }

    fun finishedLoading() {
        loadingElements--
        if (rendered && loadingElements == 0) usable.complete(Unit)
    }

    fun finishedRendering() {
        rendered = true
        if (loadingElements == 0) usable.complete(Unit)
    }
}
