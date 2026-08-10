package com.lightningkite.kiteui.models

/** Lightning Kite's brand gold, `#FCB912`. */
private val kiteUiGold: Color = Color.fromHexString("#FCB912")

/**
 * KiteUI's mark: a kite leaning into the wind, a lightning bolt in its sail, a zig-zag tail.
 *
 * ```kotlin
 * appLogo = ImageVector.kiteUiLogo()                     // brand gold, 2rem
 * appLogo = ImageVector.kiteUiLogo(Color.white, 1.5.rem) // for a dark nav bar
 * ```
 *
 * The bolt is Lightning Kite's own, lifted from its logo and scaled to sit inside the sail; the
 * lean and the gold come from there too. Everything is straight segments in a square view box, so
 * the mark comes out the same on every platform: the renderers scale x and y independently (a
 * non-square view box in a square slot would stretch it) and they disagree on both the fill rule
 * and the default line cap, neither of which this mark depends on.
 *
 * Recolor with the [color] parameter rather than [ImageVector.color] - the latter would give the
 * filled bolt a stroke and drop the stroke width from the sail and tail.
 */
public fun ImageVector.Companion.kiteUiLogo(
    color: Color = kiteUiGold,
    size: Dimension = 2.rem,
): ImageVector = ImageVector(
    width = size,
    height = size,
    viewBoxWidth = 48,
    viewBoxHeight = 48,
    paths = listOf(
        ImageVector.Path(
            fillColor = color,
            path = "M18.57,0 L12.35,17.28 L23.58,19.09 L16.61,31.8 L32.23,15.34 L19.71,13.57 L22.91,4.93 " +
                    "L41.46,4.93 L41.46,27.49 L1.25,48 L46.74,31.67 L46.74,0 Z",
        ),
    ),
)
