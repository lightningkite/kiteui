package com.lightningkite.kiteui.models

/**
 * The answers to the [MediaQuery] features that describe the device rather than the window.
 *
 * A browser resolves these itself; the native targets have to state them, because there is no
 * equivalent of a CSS media query to ask. They are constants rather than reactive values since
 * none of them can change while the app is running on the platforms that use them.
 */
internal data class MediaQueryDeviceTraits(
    val pointer: MediaQuery.Pointer.Option,
    val hover: MediaQuery.Hover.Option,
    val update: MediaQuery.Update.Option,
    val displayMode: MediaQuery.DisplayMode.Option,
)

/**
 * What Android and iOS report for the device-level features.
 *
 * Both are touch-first, so the primary pointer is coarse and there is no hover - matching what a
 * mobile browser reports on the same hardware, which is what makes shared UI code behave the same
 * way in an app as it does on the web. `Fast` update reflects that these are ordinary animating
 * displays, and `Standalone` reflects that a native app has no browser chrome around it.
 *
 * These describe the *primary* input deliberately, exactly as CSS does. A connected mouse, an iPad
 * trackpad or a stylus does not change the answer, because `pointer`/`hover` are defined in terms
 * of the primary pointing device rather than the best one available.
 *
 * `display-mode` is the one genuinely arguable choice. A native app has no browser chrome, which is
 * what `Standalone` describes, and it is what the same app reports once installed as a PWA. But an
 * ordinary (uninstalled) mobile browser tab reports `Browser`, so a query for `Standalone` matches
 * on native while the same query on the web build usually will not. `Standalone` is chosen because
 * it describes what the native app actually *is*; if you need "am I in a browser tab", that is the
 * question `Browser` answers, and it will correctly say no on native.
 */
internal val touchNativeDeviceTraits: MediaQueryDeviceTraits = MediaQueryDeviceTraits(
    pointer = MediaQuery.Pointer.Option.Coarse,
    hover = MediaQuery.Hover.Option.None,
    update = MediaQuery.Update.Option.Fast,
    displayMode = MediaQuery.DisplayMode.Option.Standalone,
)

/**
 * Evaluates a [MediaQuery] against the current window, the way a browser would.
 *
 * Exists so the native targets can answer the same questions CSS answers for the web target, and
 * so both of them answer identically - the two `shownForQuery` implementations are one line each
 * on top of this.
 */
internal fun MediaQuery.matches(
    window: WindowStatistics,
    device: MediaQueryDeviceTraits,
): Boolean = when (this) {
    // An empty `And` is vacuously true and an empty `Or` vacuously false - ordinary Boolean
    // algebra, which is also what Kotlin's all{}/any{} give us for free. CSS has no equivalent
    // to match here, since its grammar does not allow an empty condition list at all.
    is MediaQuery.And -> queries.all { it.matches(window, device) }
    is MediaQuery.Or -> queries.any { it.matches(window, device) }

    is MediaQuery.MinWidth -> window.width >= dimension
    is MediaQuery.MaxWidth -> window.width <= dimension
    is MediaQuery.MinHeight -> window.height >= dimension
    is MediaQuery.MaxHeight -> window.height <= dimension

    is MediaQuery.MinAspectRatio -> window.aspectRatio?.let { it >= ratio } ?: false
    is MediaQuery.MaxAspectRatio -> window.aspectRatio?.let { it <= ratio } ?: false

    is MediaQuery.Update -> value == device.update
    is MediaQuery.Hover -> value == device.hover
    is MediaQuery.DisplayMode -> value == device.displayMode
    is MediaQuery.Pointer -> value == device.pointer
}

/**
 * Width over height, or null when there is no meaningful ratio yet.
 *
 * No current code path produces a zero-height [WindowStatistics] - Android seeds it from
 * `displayMetrics` and iOS from `UIScreen.mainScreen.bounds`, both always non-zero - so this is
 * guarding a state we cannot presently reach rather than one observed in the wild. It is kept
 * because the alternative is a silent NaN: `0.0 / 0.0` compares false against every bound, which
 * would look like a deliberate "no match" while actually being arithmetic garbage. Reporting
 * "unknown" makes that answer intentional. This is not a fail-fast violation - an aspect ratio
 * that is not yet known is missing information, not an error.
 */
private val WindowStatistics.aspectRatio: Double?
    get() {
        val h = height.viewUnits
        return if (h <= 0.0) null else width.viewUnits / h
    }
