package com.lightningkite.kiteui.lottie.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Layer types in Lottie.
 */
object LayerType {
    const val PRECOMP = 0
    const val SOLID = 1
    const val IMAGE = 2
    const val NULL = 3
    const val SHAPE = 4
    const val TEXT = 5
    const val AUDIO = 6
    const val VIDEO_PLACEHOLDER = 7
    const val IMAGE_SEQUENCE = 8
    const val VIDEO = 9
    const val IMAGE_PLACEHOLDER = 10
    const val GUIDE = 11
    const val ADJUSTMENT = 12
    const val CAMERA = 13
    const val LIGHT = 14
}

/**
 * Matte mode types
 */
object MatteMode {
    const val NONE = 0
    const val ADD = 1
    const val INVERT = 2
    const val UNKNOWN = 3
    const val LUMA = 4
    const val LUMA_INVERTED = 5
}

/**
 * Blend mode types
 */
object BlendMode {
    const val NORMAL = 0
    const val MULTIPLY = 1
    const val SCREEN = 2
    const val OVERLAY = 3
    const val DARKEN = 4
    const val LIGHTEN = 5
    const val COLOR_DODGE = 6
    const val COLOR_BURN = 7
    const val HARD_LIGHT = 8
    const val SOFT_LIGHT = 9
    const val DIFFERENCE = 10
    const val EXCLUSION = 11
    const val HUE = 12
    const val SATURATION = 13
    const val COLOR = 14
    const val LUMINOSITY = 15
}

/**
 * A layer in a Lottie animation.
 */
@Serializable
data class LottieLayer(
    /** Layer name */
    @SerialName("nm") val name: String? = null,
    /** Layer type */
    @SerialName("ty") val type: Int = LayerType.SHAPE,
    /** Layer index (unique ID) */
    @SerialName("ind") val index: Int? = null,
    /** Parent layer index */
    @SerialName("parent") val parent: Int? = null,
    /** In-point frame */
    @SerialName("ip") val inPoint: Double = 0.0,
    /** Out-point frame */
    @SerialName("op") val outPoint: Double = 0.0,
    /** Start time */
    @SerialName("st") val startTime: Double = 0.0,
    /** 3D layer flag */
    @SerialName("ddd") val is3D: Int = 0,
    /** Transform properties */
    @SerialName("ks") val transform: LottieTransform? = null,
    /** Auto-orient flag */
    @SerialName("ao") val autoOrient: Int = 0,
    /** Blend mode */
    @SerialName("bm") val blendMode: Int = BlendMode.NORMAL,
    /** Time stretch */
    @SerialName("sr") val stretch: Double = 1.0,
    /** Hidden flag */
    @SerialName("hd") val hidden: Boolean = false,

    // Shape layer specific
    /** Shape items (for shape layers) */
    @SerialName("shapes") val shapes: List<LottieShape> = emptyList(),

    // Solid layer specific
    /** Solid color (hex string) */
    @SerialName("sc") val solidColor: String? = null,
    /** Solid width */
    @SerialName("sw") val solidWidth: Int? = null,
    /** Solid height */
    @SerialName("sh") val solidHeight: Int? = null,

    // Precomp/Image layer specific
    /** Reference ID (for precomp/image layers) */
    @SerialName("refId") val refId: String? = null,
    /** Time remapping */
    @SerialName("tm") val timeRemapping: LottieAnimatedValue? = null,

    // Text layer specific
    /** Text data */
    @SerialName("t") val textData: LottieTextData? = null,

    // Masks
    /** Mask list */
    @SerialName("masksProperties") val masks: List<LottieMask> = emptyList(),

    // Matte
    /** Track matte type */
    @SerialName("tt") val matteMode: Int? = null,
    /** Track matte target */
    @SerialName("td") val matteTarget: Int? = null,

    // Effects
    /** Layer effects */
    @SerialName("ef") val effects: List<JsonObject> = emptyList(),
)

/**
 * Mask definition
 */
@Serializable
data class LottieMask(
    /** Mask name */
    @SerialName("nm") val name: String? = null,
    /** Mask mode (a=add, s=subtract, i=intersect, l=lighten, d=darken, f=difference) */
    @SerialName("mode") val mode: String = "a",
    /** Mask path */
    @SerialName("pt") val path: LottieAnimatedShape? = null,
    /** Mask opacity */
    @SerialName("o") val opacity: LottieAnimatedValue? = null,
    /** Inverted */
    @SerialName("inv") val inverted: Boolean = false,
    /** Expansion */
    @SerialName("x") val expansion: LottieAnimatedValue? = null,
)

/**
 * Text layer data
 */
@Serializable
data class LottieTextData(
    @SerialName("d") val document: LottieTextDocument? = null,
    @SerialName("a") val animators: List<JsonObject> = emptyList(),
)

/**
 * Text document
 */
@Serializable
data class LottieTextDocument(
    @SerialName("k") val keyframes: List<LottieTextKeyframe> = emptyList(),
)

/**
 * Text keyframe
 */
@Serializable
data class LottieTextKeyframe(
    @SerialName("t") val time: Double = 0.0,
    @SerialName("s") val value: LottieTextValue? = null,
)

/**
 * Text value
 */
@Serializable
data class LottieTextValue(
    @SerialName("t") val text: String? = null,
    @SerialName("f") val fontFamily: String? = null,
    @SerialName("s") val fontSize: Double? = null,
    @SerialName("fc") val fillColor: List<Double>? = null,
    @SerialName("sc") val strokeColor: List<Double>? = null,
    @SerialName("sw") val strokeWidth: Double? = null,
    @SerialName("j") val justification: Int = 0,
    @SerialName("lh") val lineHeight: Double? = null,
    @SerialName("ls") val letterSpacing: Double? = null,
)
