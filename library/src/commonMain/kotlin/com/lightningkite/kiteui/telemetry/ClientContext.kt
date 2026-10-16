package com.lightningkite.kiteui.telemetry

import com.lightningkite.services.SettingContext
import com.lightningkite.services.SharedResources
import com.lightningkite.services.telemetry.TelemetryBackend
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * The [SettingContext] for client-side [com.lightningkite.services.Namespaced] owners, so app code records
 * telemetry the same way server code does:
 *
 * ```kotlin
 * object Checkout : Namespaced {
 *     override val name = "checkout"
 *     override val context = ClientContext
 * }
 * Checkout.telemetryTrace("submit") { ... }
 * ```
 *
 * Its [telemetryBackend] is the installed [Telemetry], or [TelemetryBackend.Noop] when none is. Counters and
 * histograms capture the backend when they're created, so create them after [Telemetry.install].
 */
public object ClientContext : SettingContext {
    // Unused on clients; the server identifies the app from the batch's resource attributes.
    override val projectName: String get() = ""
    override val publicUrl: String get() = ""
    override val internalSerializersModule: SerializersModule get() = EmptySerializersModule()
    override val sharedResources: SharedResources = SharedResources()
    override var telemetryBackend: TelemetryBackend = TelemetryBackend.Noop
        internal set
}
