package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.LiveRegionMode
import com.lightningkite.kiteui.views.*

/**
 * Marks the next element as a heading at the given [level] (1–6) for assistive technology navigation.
 *
 * ```kotlin
 * heading(1) - text("Page Title")
 * heading(2) - text("Section Title")
 * ```
 */
@ViewModifierDsl3
fun ElementWriter.CanAddTheme.heading(level: Int): ElementWriter.CanAddTheme = asHeading(level)

/**
 * Marks the next element as a live region so screen readers announce content changes.
 *
 * ```kotlin
 * liveRegion() - col { text { ::content { statusMessage() } } }
 * ```
 */
@ViewModifierDsl3
fun ElementWriter.CanAddTheme.liveRegion(mode: LiveRegionMode = LiveRegionMode.Polite): ElementWriter.CanAddTheme =
    beforeSetup { accessibleLiveRegion = mode }
