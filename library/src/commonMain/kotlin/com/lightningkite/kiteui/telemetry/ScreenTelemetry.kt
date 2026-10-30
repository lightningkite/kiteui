package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.ContextAddon
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.contextAddon
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.CompletableDeferred
import kotlin.random.Random

// Claude: the UI code (NativeElement, navigatorView) may only use what's in this file, never Telemetry itself, so an
// app that never installs telemetry doesn't ship it.

/**
 * The attributes this context adds to everything recorded inside it, such as spans from the requests and actions its
 * elements start and errors they report.
 *
 * Each context holds only its own: a record gets those of every context from the root down to where it was made, with
 * a value set further down replacing one from above. So `context.telemetryAttributes += ...` in a container adds to
 * what's above without copying it, and a later change at the root still reaches everything. They are read each time
 * something is recorded, so a change applies to everything recorded after it.
 *
 * Set them on the root context for the whole app, or on a container for what's inside it. A leaf element's context
 * is its container's, so attributes a leaf sets apply to its siblings too.
 */
public var ElementContext.telemetryAttributes: TelemetryAttributes by contextAddon(TelemetryAttributes.empty, ContextAddon.Scope.Local)

internal var ElementContext.screenVisit: ScreenVisit? by contextAddon(null)

/** Records a screen visit for as long as it runs; [Telemetry.install] sets it. */
internal var screenVisitRecorder: (suspend (PageNavigator, ScreenVisit) -> Unit)? = null

/**
 * One showing of a screen, which is usable once it has rendered and none of its elements is loading. Elements
 * report through [startedLoading]/[finishedLoading] when their background processes become not-ready and settle again.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class ScreenVisit(screenName: String) {
    val attributes: TelemetryAttributes = telemetryAttributesOf(
        TelemetryKeys.App.screenName to screenName,
        TelemetryKeys.App.screenId to Random.nextBytes(8).toHexString(),
    )

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
