package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ImageVector
import com.lightningkite.kiteui.models.rem

/**
 * Lightning Kite's logo mark, traced from the SVG at lightningkite.com and rescaled into a square
 * view box - the renderers scale x and y independently, so the original 36.742 x 38.772 box would
 * stretch the mark in a square slot.
 *
 * This lives in the example app rather than the library: it is Lightning Kite's mark, not KiteUI's.
 * For KiteUI's own mark use [ImageVector.kiteUiLogo].
 */
fun lightningKiteLogo(
    color: Color = Color.fromHexString("#FCB912"),
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
