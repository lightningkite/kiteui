package com.lightningkite.kiteui.lottie.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Top-level Lottie animation data structure.
 * Represents the complete parsed Lottie JSON file.
 */
@Serializable
data class LottieAnimation(
    /** Animation name */
    @SerialName("nm") val name: String? = null,
    /** Lottie format version */
    @SerialName("v") val version: String? = null,
    /** Frame rate (frames per second) */
    @SerialName("fr") val frameRate: Double = 30.0,
    /** In-point (first frame) */
    @SerialName("ip") val inPoint: Double = 0.0,
    /** Out-point (last frame) */
    @SerialName("op") val outPoint: Double = 0.0,
    /** Canvas width */
    @SerialName("w") val width: Int = 0,
    /** Canvas height */
    @SerialName("h") val height: Int = 0,
    /** 3D layer flag */
    @SerialName("ddd") val is3D: Int = 0,
    /** List of layers */
    @SerialName("layers") val layers: List<LottieLayer> = emptyList(),
    /** Assets (images, precomps) */
    @SerialName("assets") val assets: List<LottieAsset> = emptyList(),
    /** Fonts */
    @SerialName("fonts") val fonts: LottieFontList? = null,
    /** Markers */
    @SerialName("markers") val markers: List<LottieMarker> = emptyList(),
) {
    /** Total duration in seconds */
    val durationSeconds: Double get() = (outPoint - inPoint) / frameRate

    /** Total number of frames */
    val totalFrames: Double get() = outPoint - inPoint

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun parse(jsonString: String): LottieAnimation = json.decodeFromString(jsonString)
    }
}

/**
 * Asset reference in a Lottie animation (images, precomps, etc.)
 */
@Serializable
data class LottieAsset(
    /** Asset ID */
    @SerialName("id") val id: String,
    /** Width (for images) */
    @SerialName("w") val width: Int? = null,
    /** Height (for images) */
    @SerialName("h") val height: Int? = null,
    /** Image path/URL */
    @SerialName("u") val path: String? = null,
    /** Image filename */
    @SerialName("p") val fileName: String? = null,
    /** Embedded image data (base64) */
    @SerialName("e") val embedded: Int? = null,
    /** Layers for precomp assets */
    @SerialName("layers") val layers: List<LottieLayer>? = null,
)

/**
 * Font list container
 */
@Serializable
data class LottieFontList(
    @SerialName("list") val list: List<LottieFont> = emptyList()
)

/**
 * Font definition
 */
@Serializable
data class LottieFont(
    @SerialName("fName") val name: String? = null,
    @SerialName("fFamily") val family: String? = null,
    @SerialName("fStyle") val style: String? = null,
    @SerialName("ascent") val ascent: Double? = null,
)

/**
 * Marker for named positions in the animation timeline
 */
@Serializable
data class LottieMarker(
    @SerialName("tm") val time: Double = 0.0,
    @SerialName("cm") val comment: String? = null,
    @SerialName("dr") val duration: Double? = null,
)
