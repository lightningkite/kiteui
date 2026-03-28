package com.lightningkite.kiteui.models

import kotlinx.datetime.format.Padding
import kotlin.random.Random

/**
 * Creates a neumorphism-style theme with soft, extruded appearance using dual shadows.
 *
 * Neumorphism (new skeuomorphism) creates a soft, 3D appearance using:
 * - Two shadows: A light shadow (top-left) and dark shadow (bottom-right)
 * - Background colors that closely match parent backgrounds
 * - Convex (raised) appearance for normal state
 * - Concave (inset) appearance for pressed/active states
 *
 * @param id Unique identifier for this theme.
 * @param baseColor The base background color. Should be a neutral gray for best effect.
 * @param accentColor The accent color for important elements.
 * @param accentForeground The foreground color for accent elements.
 * @param lightShadowColor The light/highlight shadow color (usually white with alpha).
 * @param darkShadowColor The dark shadow color (usually black with alpha).
 * @param shadowDistance The offset distance for shadows.
 * @param shadowBlur The blur radius for shadows.
 * @param title Font styling for headers.
 * @param body Font styling for body text.
 * @param cornerRadii The corner radius configuration.
 * @param gap The spacing between elements.
 */
fun Theme.Companion.neumorphism(
    id: String,
    baseColor: Color = Color.gray(0.9f),
    accentColor: Color = Color.fromHex(0xFF6200EE.toInt()),
    accentForeground: Color = if (accentColor.perceivedBrightness < 0.6f) Color.white else Color.black,
    lightShadowColor: Color = Color.white.applyAlpha(0.7f),
    darkShadowColor: Color = Color.black.applyAlpha(0.15f),
    shadowDistance: Dimension = 8.dp,
    shadowBlur: Dimension = 16.dp,
    title: FontAndStyle = FontAndStyle(systemDefaultFont),
    body: FontAndStyle = FontAndStyle(systemDefaultFont),
    cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
    gap: Dimension = 1.rem,
): Theme {
    val foreground = if (baseColor.perceivedBrightness > 0.5f)
        Color.black.applyAlpha(0.8f)
    else
        Color.white.applyAlpha(0.9f)

    val convexShadows = Shadow.neumorphicConvex(
        distance = shadowDistance,
        blur = shadowBlur,
        lightColor = lightShadowColor,
        darkColor = darkShadowColor
    )

    val concaveShadows = Shadow.neumorphicConcave(
        distance = shadowDistance / 2f,
        blur = shadowBlur / 2f,
        lightColor = lightShadowColor,
        darkColor = darkShadowColor
    )

    return Theme(
        id = id,
        font = body,
        elevation = 0.px, // Disable elevation-based shadows
        shadows = null, // Base theme has no shadows (they're applied via semantics)
        cornerRadii = cornerRadii,
        gap = gap,
        outline = baseColor.highlight(0.05f),
        outlineWidth = 0.px,
        foreground = foreground,
        background = baseColor,
        semanticOverrides = SemanticOverrides(
            OuterSemantic.override {
                it.alter(
                    cascading = false,
                    gap = 0.px,
                    cornerRadii = CornerRadii.Fixed(0.px),
                    outlineWidth = 0.px,
                    shadows = null,
                ).withBackNoPadding
            },
            MainContentSemantic.override {
                it.withBack(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.px),
                    outlineWidth = 0.px,
                    shadows = null,
                )
            },
            BarSemantic.override {
                it.withBack(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.px),
                    outlineWidth = 0.px,
                    shadows = null,
                )
            },
            HeaderSemantic.override {
                it.withoutBack(font = title)
            },
            CardSemantic.override {
                it.withBack(
                    shadows = convexShadows,
                )
            },
            FieldSemantic.override {
                it.withBack(
                    cascading = false,
                    shadows = concaveShadows,
                    cornerRadii = when (val base = it.cornerRadii) {
                        is CornerRadii.AdaptiveToSpacing -> CornerRadii.Fixed(base.value)
                        is CornerRadii.Fixed -> base
                        is CornerRadii.RatioOfSize -> base
                        is CornerRadii.RatioOfSpacing -> CornerRadii.Fixed(it.gap * base.value)
                        is CornerRadii.PerCorner -> base
                    }
                )
            },
            ButtonSemantic.override {
                it.withBack(
                    shadows = convexShadows,
                )
            },
            HoverSemantic.override {
                // Slightly stronger shadows on hover
                val hoverShadows = Shadow.neumorphicConvex(
                    distance = shadowDistance * 1.2f,
                    blur = shadowBlur * 1.2f,
                    lightColor = lightShadowColor,
                    darkColor = darkShadowColor
                )
                it.withBack(
                    shadows = hoverShadows,
                    background = it.background.map { c -> c.highlight(0.02f) },
                )
            },
            DownSemantic.override {
                it.withBack(
                    shadows = concaveShadows,
                    background = it.background.map { c -> c.highlight(-0.02f) },
                )
            },
            SelectedSemantic.override {
                it.withBack(
                    shadows = concaveShadows,
                    background = it.background.map { c -> c.highlight(-0.05f) },
                    outlineWidth = 0.px,
                )
            },
            UnselectedSemantic.override {
                it.withBack(
                    shadows = convexShadows,
                    outlineWidth = 0.px,
                )
            },
            FocusSemantic.override {
                val focusShadows = Shadow.neumorphicConvex(
                    distance = shadowDistance * 1.2f,
                    blur = shadowBlur * 1.2f,
                    lightColor = lightShadowColor,
                    darkColor = darkShadowColor
                )
                it.withBack(
                    shadows = focusShadows,
                    outlineWidth = 2.dp,
                    outline = accentColor.applyAlpha(0.5f),
                )
            },
            DisabledSemantic.override {
                // Flatter appearance for disabled
                val disabledShadows = Shadow.neumorphicConvex(
                    distance = shadowDistance / 2f,
                    blur = shadowBlur / 2f,
                    lightColor = lightShadowColor.applyAlpha(0.3f),
                    darkColor = darkShadowColor.applyAlpha(0.08f)
                )
                it.withBack(
                    shadows = disabledShadows,
                    foreground = it.foreground.applyAlpha(0.4f),
                    background = it.background.applyAlpha(0.7f),
                )
            },
            // Subtle tint semantics: mix base color with ~10% semantic color
            // to maintain the neumorphic material illusion
            ImportantSemantic.override {
                val tintedBg = Color.interpolate(baseColor, accentColor, 0.10f)
                it.withBack(
                    foreground = foreground,
                    background = tintedBg,
                    shadows = convexShadows,
                    semanticOverrides = SemanticOverrides(
                        HoverSemantic.override { inner ->
                            inner.withBack(
                                shadows = Shadow.neumorphicConvex(
                                    distance = shadowDistance * 1.2f,
                                    blur = shadowBlur * 1.2f,
                                    lightColor = lightShadowColor,
                                    darkColor = darkShadowColor
                                ),
                                background = tintedBg.highlight(0.02f),
                            )
                        },
                        DownSemantic.override { inner ->
                            inner.withBack(
                                shadows = concaveShadows,
                                background = tintedBg.highlight(-0.02f),
                            )
                        },
                    )
                )
            },
            CriticalSemantic.override {
                val tintedBg = Color.interpolate(baseColor, accentColor, 0.15f)
                it.withBack(
                    foreground = foreground,
                    background = tintedBg,
                    shadows = convexShadows,
                    semanticOverrides = SemanticOverrides(
                        HoverSemantic.override { inner ->
                            inner.withBack(
                                shadows = Shadow.neumorphicConvex(
                                    distance = shadowDistance * 1.2f,
                                    blur = shadowBlur * 1.2f,
                                    lightColor = lightShadowColor,
                                    darkColor = darkShadowColor
                                ),
                                background = tintedBg.highlight(0.02f),
                            )
                        },
                        DownSemantic.override { inner ->
                            inner.withBack(
                                shadows = concaveShadows,
                                background = tintedBg.highlight(-0.02f),
                            )
                        },
                    )
                )
            },
            WarningSemantic.override {
                val warningColor = Color.fromHex(0xFFe36e24.toInt())
                val tintedBg = Color.interpolate(baseColor, warningColor, 0.10f)
                it.withBack(
                    foreground = foreground,
                    background = tintedBg,
                    shadows = convexShadows,
                    semanticOverrides = SemanticOverrides(
                        HoverSemantic.override { inner ->
                            inner.withBack(
                                shadows = Shadow.neumorphicConvex(
                                    distance = shadowDistance * 1.2f,
                                    blur = shadowBlur * 1.2f,
                                    lightColor = lightShadowColor,
                                    darkColor = darkShadowColor
                                ),
                                background = tintedBg.highlight(0.02f),
                            )
                        },
                        DownSemantic.override { inner ->
                            inner.withBack(
                                shadows = concaveShadows,
                                background = tintedBg.highlight(-0.02f),
                            )
                        },
                    )
                )
            },
            DangerSemantic.override {
                val dangerColor = Color.fromHex(0xFFB00020.toInt())
                val tintedBg = Color.interpolate(baseColor, dangerColor, 0.10f)
                it.withBack(
                    foreground = foreground,
                    background = tintedBg,
                    shadows = convexShadows,
                    semanticOverrides = SemanticOverrides(
                        HoverSemantic.override { inner ->
                            inner.withBack(
                                shadows = Shadow.neumorphicConvex(
                                    distance = shadowDistance * 1.2f,
                                    blur = shadowBlur * 1.2f,
                                    lightColor = lightShadowColor,
                                    darkColor = darkShadowColor
                                ),
                                background = tintedBg.highlight(0.02f),
                            )
                        },
                        DownSemantic.override { inner ->
                            inner.withBack(
                                shadows = concaveShadows,
                                background = tintedBg.highlight(-0.02f),
                            )
                        },
                    )
                )
            },
            AffirmativeSemantic.override {
                val affirmativeColor = Color.fromHex(0xFF20a020.toInt())
                val tintedBg = Color.interpolate(baseColor, affirmativeColor, 0.10f)
                it.withBack(
                    foreground = foreground,
                    background = tintedBg,
                    shadows = convexShadows,
                    semanticOverrides = SemanticOverrides(
                        HoverSemantic.override { inner ->
                            inner.withBack(
                                shadows = Shadow.neumorphicConvex(
                                    distance = shadowDistance * 1.2f,
                                    blur = shadowBlur * 1.2f,
                                    lightColor = lightShadowColor,
                                    darkColor = darkShadowColor
                                ),
                                background = tintedBg.highlight(0.02f),
                            )
                        },
                        DownSemantic.override { inner ->
                            inner.withBack(
                                shadows = concaveShadows,
                                background = tintedBg.highlight(-0.02f),
                            )
                        },
                    )
                )
            },
            DismissSemantic.override {
                val isDark = baseColor.perceivedBrightness < 0.5f
                it.withBack(
                    cascading = false,
                    gap = 0.dp,
                    cornerRadii = CornerRadii.AdaptiveToSpacing(0.dp),
                    background = if (isDark) lightShadowColor.invert().applyAlpha(1f) else lightShadowColor.applyAlpha(
                        1f
                    ),
                    shadows = null,
                )
            },
            DialogSemantic.override {
                val isDark = baseColor.perceivedBrightness < 0.5f
                val dialogShadows = Shadow.neumorphicConvex(
                    distance = shadowDistance * 2.5f,
                    blur = shadowBlur * 3f,
                    lightColor = if (isDark) lightShadowColor.copy(alpha = (lightShadowColor.alpha * 2f).coerceAtMost(1f)) else lightShadowColor,
                    darkColor = if (isDark) Color.black.applyAlpha(0.5f) else Color.black.applyAlpha(0.25f)
                )
                val dialogCornerRadii = CornerRadii.Fixed(gap)
                it.withBack(
                    cornerRadii = dialogCornerRadii,
                    shadows = dialogShadows,
                    background = if (isDark) baseColor.highlight(0.05f) else null,
                    semanticOverrides = SemanticOverrides(
                        // The first card inside the dialog gets dialog-level shadows
                        CardSemantic.override { inner ->
                            inner.withBack(
                                cornerRadii = dialogCornerRadii,
                                shadows = dialogShadows,
                                semanticOverrides = SemanticOverrides(
                                    // Nested cards inside the dialog card revert to normal convex shadows
                                    CardSemantic.override { nested ->
                                        nested.withBack(
                                            shadows = convexShadows,
                                        )
                                    },
                                )
                            )
                        },
                    )
                )
            },
            PopoverSemantic.override {
                it.withBack(
                    shadows = Shadow.neumorphicConvex(
                        distance = shadowDistance * 1.5f,
                        blur = shadowBlur * 1.5f,
                        lightColor = lightShadowColor,
                        darkColor = darkShadowColor
                    ),
                )
            },
        ),
    )
}

/**
 * Convenience object for creating neumorphism themes.
 */
object NeumorphismTheme {
    operator fun invoke(
        id: String,
        baseColor: Color = Color.gray(0.9f),
        accentColor: Color = Color.fromHex(0xFF6200EE.toInt()),
        accentForeground: Color = if (accentColor.perceivedBrightness < 0.6f) Color.white else Color.black,
        lightShadowColor: Color = Color.white.applyAlpha(0.7f),
        darkShadowColor: Color = Color.black.applyAlpha(0.15f),
        shadowDistance: Dimension = 8.dp,
        shadowBlur: Dimension = 16.dp,
        title: FontAndStyle = FontAndStyle(systemDefaultFont),
        body: FontAndStyle = FontAndStyle(systemDefaultFont),
        cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
        gap: Dimension = 1.rem,
    ) = Theme.neumorphism(
        id = id,
        baseColor = baseColor,
        accentColor = accentColor,
        accentForeground = accentForeground,
        lightShadowColor = lightShadowColor,
        darkShadowColor = darkShadowColor,
        shadowDistance = shadowDistance,
        shadowBlur = shadowBlur,
        title = title,
        body = body,
        cornerRadii = cornerRadii,
        gap = gap,
    )

    /**
     * Creates a light neumorphism theme with a gray base.
     */
    fun light(
        id: String = "neumorphism-light",
        accentColor: Color = Color.fromHex(0xFF6200EE.toInt()),

        ): Theme {
        val shadowDistance = (1..20).random()
        return this(
            id = id,
            baseColor = Color.gray(0.9f),
            accentColor = accentColor,
            lightShadowColor = Color.white.applyAlpha(0.8f),
            darkShadowColor = Color.black.applyAlpha(0.12f),
//        shadowDistance = (1..25).random().dp,
            shadowDistance = shadowDistance.dp,
//            shadowBlur = (15..50).random().dp,
            shadowBlur = (shadowDistance..30).random().dp
        )
    }

    /**
     * Creates a dark neumorphism theme.
     */
    fun dark(
        id: String = "neumorphism-dark",
        accentColor: Color = Color.fromHex(0xFF03DAC6.toInt()),
    ) = this(
        id = id,
        baseColor = Color.gray(0.25f),
        accentColor = accentColor,
        lightShadowColor = Color.white.applyAlpha(0.08f),
        darkShadowColor = Color.black.applyAlpha(0.4f),
    )
}
