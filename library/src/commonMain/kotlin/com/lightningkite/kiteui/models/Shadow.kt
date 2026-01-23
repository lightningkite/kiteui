package com.lightningkite.kiteui.models

/**
 * Represents a shadow effect that can be applied to UI elements.
 *
 * Unlike the single elevation-based shadow system, this allows for multiple shadows
 * with precise control over each shadow's properties. This is essential for neumorphism
 * which requires two shadows (light highlight + dark shadow).
 *
 * @property offsetX Horizontal offset of the shadow. Positive values move right, negative left.
 * @property offsetY Vertical offset of the shadow. Positive values move down, negative up.
 * @property blurRadius The blur radius. Larger values create softer, more diffuse shadows.
 * @property spreadRadius The spread radius. Positive values expand the shadow, negative values shrink it.
 * @property color The shadow color, typically with alpha for transparency.
 * @property inset When true, the shadow is drawn inside the element (concave effect).
 */
data class Shadow(
    val offsetX: Dimension = 0.px,
    val offsetY: Dimension = 0.px,
    val blurRadius: Dimension = 0.px,
    val spreadRadius: Dimension = 0.px,
    val color: Color = Color.black.applyAlpha(0.5f),
    val inset: Boolean = false
) {
    companion object {
        /**
         * Creates a pair of shadows for a neumorphic convex (raised) effect.
         *
         * @param distance The offset distance for shadows.
         * @param blur The blur radius.
         * @param lightColor The highlight shadow color (typically light/white with alpha).
         * @param darkColor The shadow color (typically dark with alpha).
         */
        fun neumorphicConvex(
            distance: Dimension = 8.dp,
            blur: Dimension = 16.dp,
            lightColor: Color = Color.white.applyAlpha(0.7f),
            darkColor: Color = Color.black.applyAlpha(0.15f)
        ): List<Shadow> = listOf(
            Shadow(
                offsetX = distance * -1f,
                offsetY = distance * -1f,
                blurRadius = blur,
                color = lightColor
            ),
            Shadow(
                offsetX = distance,
                offsetY = distance,
                blurRadius = blur,
                color = darkColor
            )
        )

        /**
         * Creates a pair of inset shadows for a neumorphic concave (pressed) effect.
         *
         * @param distance The offset distance for shadows.
         * @param blur The blur radius.
         * @param lightColor The highlight shadow color (typically light/white with alpha).
         * @param darkColor The shadow color (typically dark with alpha).
         */
        fun neumorphicConcave(
            distance: Dimension = 4.dp,
            blur: Dimension = 8.dp,
            lightColor: Color = Color.white.applyAlpha(0.7f),
            darkColor: Color = Color.black.applyAlpha(0.15f)
        ): List<Shadow> = listOf(
            Shadow(
                offsetX = distance * -1f,
                offsetY = distance * -1f,
                blurRadius = blur,
                color = darkColor,
                inset = true
            ),
            Shadow(
                offsetX = distance,
                offsetY = distance,
                blurRadius = blur,
                color = lightColor,
                inset = true
            )
        )
    }
}
