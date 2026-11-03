package com.lightningkite.kiteui.models

/** The three golds of the KiteUI mark, from the brand SVG at the repository root. */
private val kiteUiSailGold: Color = Color.fromHexString("#FDBA12")
private val kiteUiNearTrailGold: Color = Color.fromHexString("#FFC933")
private val kiteUiFarTrailGold: Color = Color.fromHexString("#BF8C00")

/** How far back the second chevron sits - the mark's only depth cue, so recoloring keeps it. */
private const val FAR_TRAIL_OPACITY = 0.5f

/**
 * KiteUI's mark: a kite climbing to the right, trailing two chevrons of motion.
 *
 * ```kotlin
 * appLogo = ImageVector.kiteUiLogo()                     // full brand color, 2rem
 * appLogo = ImageVector.kiteUiLogo(Color.white, 1.5.rem) // monochrome, for a dark nav bar
 * ```
 *
 * Traced from `logo.svg` at the repository root, view box and all, so the mark keeps the margin the
 * designer drew around it. Pass [color] to flatten the three golds to one hue for a background they
 * would otherwise disappear into; the far chevron stays half transparent either way.
 *
 * Recolor with [color] rather than [ImageVector.color] - the latter puts one paint on every path,
 * which would flatten the trail into the kite.
 *
 * Everything is straight segments in a square view box, so the mark comes out the same on every
 * platform: the renderers scale x and y independently (a non-square view box in a square slot would
 * stretch it) and they disagree on the fill rule, which no path here depends on.
 */
public fun ImageVector.Companion.kiteUiLogo(
    color: Color? = null,
    size: Dimension = 2.rem,
): ImageVector = ImageVector(
    width = size,
    height = size,
    viewBoxWidth = 1000,
    viewBoxHeight = 1000,
    paths = listOf(
        ImageVector.Path(
            fillColor = color ?: kiteUiNearTrailGold,
            path = "M308.5,404.2 L540.9,117.3 L494.8,63.2 L227.2,393.6 L509.6,928.5 L545.4,852.7 Z",
        ),
        ImageVector.Path(
            fillColor = (color ?: kiteUiFarTrailGold).applyAlpha(FAR_TRAIL_OPACITY),
            path = "M149.4,402.7 L381.8,115.7 L335.7,61.6 L68.1,392.1 L350.5,926.9 L386.3,851.2 Z",
        ),
        ImageVector.Path(
            fillColor = color ?: kiteUiSailGold,
            path = "M659.7,73.1 L392,403.5 L674.4,938.4 L931.9,392.8 Z",
        ),
    ),
)
