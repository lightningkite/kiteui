// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw

/**
 * Compresses and resizes this image to fit within the given dimensions, preserving aspect ratio.
 * Returns an ImageRaw containing JPEG-encoded data suitable for display or upload.
 *
 * @param maxWidth Maximum width in pixels. Image will be scaled down if wider.
 * @param maxHeight Maximum height in pixels. Image will be scaled down if taller.
 * @param quality JPEG compression quality, 0.0 (smallest) to 1.0 (highest).
 * @throws IllegalArgumentException if the file is not a decodable image.
 */
expect suspend fun ImageLocal.compressed(
    maxWidth: Int = 2048,
    maxHeight: Int = 2048,
    quality: Float = 0.8f
): ImageRaw

/**
 * Calculates target dimensions that fit within maxWidth x maxHeight while preserving aspect ratio.
 * Returns the original dimensions unchanged if the image is already within bounds.
 */
internal fun calculateScaledSize(
    originalWidth: Int,
    originalHeight: Int,
    maxWidth: Int,
    maxHeight: Int
): Pair<Int, Int> {
    if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
        return originalWidth to originalHeight
    }
    val scale = minOf(
        maxWidth.toFloat() / originalWidth,
        maxHeight.toFloat() / originalHeight
    )
    return (originalWidth * scale).toInt() to (originalHeight * scale).toInt()
}
