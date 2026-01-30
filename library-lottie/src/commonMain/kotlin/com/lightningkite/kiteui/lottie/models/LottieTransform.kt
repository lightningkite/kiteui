package com.lightningkite.kiteui.lottie.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Transform properties for a layer or shape.
 * Contains position, anchor point, scale, rotation, opacity, and skew.
 */
@Serializable
data class LottieTransform(
    /** Anchor point */
    @SerialName("a") val anchorPoint: LottieAnimatedMultiValue? = null,
    /** Position */
    @SerialName("p") val position: LottieAnimatedMultiValue? = null,
    /** Position X (when position is split) */
    @SerialName("px") val positionX: LottieAnimatedValue? = null,
    /** Position Y (when position is split) */
    @SerialName("py") val positionY: LottieAnimatedValue? = null,
    /** Position Z (for 3D) */
    @SerialName("pz") val positionZ: LottieAnimatedValue? = null,
    /** Scale */
    @SerialName("s") val scale: LottieAnimatedMultiValue? = null,
    /** Rotation (in degrees) */
    @SerialName("r") val rotation: LottieAnimatedValue? = null,
    /** Rotation X (for 3D) */
    @SerialName("rx") val rotationX: LottieAnimatedValue? = null,
    /** Rotation Y (for 3D) */
    @SerialName("ry") val rotationY: LottieAnimatedValue? = null,
    /** Rotation Z (alternative to r, same meaning) */
    @SerialName("rz") val rotationZ: LottieAnimatedValue? = null,
    /** Opacity (0-100) */
    @SerialName("o") val opacity: LottieAnimatedValue? = null,
    /** Skew */
    @SerialName("sk") val skew: LottieAnimatedValue? = null,
    /** Skew axis */
    @SerialName("sa") val skewAxis: LottieAnimatedValue? = null,
    /** Orientation (for 3D) */
    @SerialName("or") val orientation: LottieAnimatedMultiValue? = null,
)

/**
 * An animated single value (scalar).
 * Can be static or have keyframes.
 */
@Serializable(with = LottieAnimatedValueSerializer::class)
data class LottieAnimatedValue(
    /** Whether the value is animated */
    val animated: Boolean = false,
    /** Static value (when not animated) */
    val staticValue: Double = 0.0,
    /** Keyframes (when animated) */
    val keyframes: List<LottieKeyframe> = emptyList(),
    /** Expression */
    val expression: String? = null,
)

/**
 * An animated multi-dimensional value (point, scale, etc.).
 * Can be static or have keyframes.
 */
@Serializable(with = LottieAnimatedMultiValueSerializer::class)
data class LottieAnimatedMultiValue(
    /** Whether the value is animated */
    val animated: Boolean = false,
    /** Static value (when not animated) */
    val staticValue: List<Double> = listOf(0.0, 0.0),
    /** Keyframes (when animated) */
    val keyframes: List<LottieMultiKeyframe> = emptyList(),
    /** Expression */
    val expression: String? = null,
)

/**
 * An animated shape/path.
 */
@Serializable(with = LottieAnimatedShapeSerializer::class)
data class LottieAnimatedShape(
    /** Whether the shape is animated */
    val animated: Boolean = false,
    /** Static shape (when not animated) */
    val staticValue: LottieBezierPath? = null,
    /** Keyframes (when animated) */
    val keyframes: List<LottieShapeKeyframe> = emptyList(),
)

/**
 * A keyframe for a single value.
 */
@Serializable
data class LottieKeyframe(
    /** Time in frames */
    @SerialName("t") val time: Double = 0.0,
    /** Value at this keyframe */
    @SerialName("s") val startValue: List<Double>? = null,
    /** End value (deprecated, use next keyframe's s) */
    @SerialName("e") val endValue: List<Double>? = null,
    /** Hold keyframe (no interpolation) */
    @SerialName("h") val hold: Int = 0,
    /** In tangent (bezier easing) */
    @SerialName("i") val inTangent: LottieEasing? = null,
    /** Out tangent (bezier easing) */
    @SerialName("o") val outTangent: LottieEasing? = null,
)

/**
 * A keyframe for multi-dimensional values.
 */
@Serializable
data class LottieMultiKeyframe(
    /** Time in frames */
    @SerialName("t") val time: Double = 0.0,
    /** Value at this keyframe */
    @SerialName("s") val startValue: List<Double>? = null,
    /** End value (deprecated) */
    @SerialName("e") val endValue: List<Double>? = null,
    /** Hold keyframe */
    @SerialName("h") val hold: Int = 0,
    /** In tangent */
    @SerialName("i") val inTangent: LottieEasing? = null,
    /** Out tangent */
    @SerialName("o") val outTangent: LottieEasing? = null,
    /** Spatial in tangent (for position) */
    @SerialName("ti") val spatialInTangent: List<Double>? = null,
    /** Spatial out tangent (for position) */
    @SerialName("to") val spatialOutTangent: List<Double>? = null,
)

/**
 * A keyframe for shapes/paths.
 */
@Serializable
data class LottieShapeKeyframe(
    /** Time in frames */
    @SerialName("t") val time: Double = 0.0,
    /** Shape at this keyframe */
    @SerialName("s") val startValue: List<LottieBezierPath>? = null,
    /** End shape (deprecated) */
    @SerialName("e") val endValue: List<LottieBezierPath>? = null,
    /** Hold keyframe */
    @SerialName("h") val hold: Int = 0,
    /** In tangent */
    @SerialName("i") val inTangent: LottieEasing? = null,
    /** Out tangent */
    @SerialName("o") val outTangent: LottieEasing? = null,
)

/**
 * Bezier easing definition.
 */
@Serializable
data class LottieEasing(
    /** X coordinates of bezier control points */
    @SerialName("x") val x: JsonElement? = null,
    /** Y coordinates of bezier control points */
    @SerialName("y") val y: JsonElement? = null,
) {
    fun getX(): List<Double> = parseToDoubleList(x)
    fun getY(): List<Double> = parseToDoubleList(y)

    private fun parseToDoubleList(element: JsonElement?): List<Double> {
        return when (element) {
            is JsonArray -> element.map {
                when (it) {
                    is JsonPrimitive -> it.doubleOrNull ?: 0.0
                    else -> 0.0
                }
            }
            is JsonPrimitive -> listOf(element.doubleOrNull ?: 0.0)
            else -> listOf(0.0)
        }
    }
}

/**
 * Bezier path data (vertices and tangents).
 */
@Serializable
data class LottieBezierPath(
    /** Closed path flag */
    @SerialName("c") val closed: Boolean = false,
    /** Vertices */
    @SerialName("v") val vertices: List<List<Double>> = emptyList(),
    /** In tangents (relative to vertices) */
    @SerialName("i") val inTangents: List<List<Double>> = emptyList(),
    /** Out tangents (relative to vertices) */
    @SerialName("o") val outTangents: List<List<Double>> = emptyList(),
)
