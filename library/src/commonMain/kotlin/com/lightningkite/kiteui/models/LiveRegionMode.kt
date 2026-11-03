package com.lightningkite.kiteui.models

/**
 * Controls how assistive technologies announce dynamic content changes in a region.
 *
 * Maps to `aria-live` on web, `accessibilityLiveRegion` on Android,
 * and `UIAccessibility` notifications on iOS.
 */
public enum class LiveRegionMode {
    /** No live region announcements (default). */
    None,
    /** Announces updates at the next graceful opportunity (e.g., end of current speech). */
    Polite,
    /** Interrupts current speech to announce updates immediately. Use sparingly. */
    Assertive,
}
