package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.views.ContextAddon
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.contextAddon
import com.lightningkite.services.telemetry.TelemetryAttributes

/**
 * The attributes this context adds to everything recorded inside it, such as spans from the requests and actions its
 * elements start and errors they report.
 *
 * Each context holds only its own: a record gets those of every context from the root down to where it was made, with
 * a value set further down replacing one from above. So `context.telemetryAttributes += ...` in a container adds to
 * what's above without copying it. They are read each time something is recorded, so a change applies to everything
 * recorded after it. For attributes of the whole app, use [Telemetry.attributes].
 *
 * A leaf element's context is its container's, so attributes a leaf sets apply to its siblings too.
 */
public var ElementContext.telemetryAttributes: TelemetryAttributes by contextAddon(TelemetryAttributes.empty, ContextAddon.Scope.Local)
