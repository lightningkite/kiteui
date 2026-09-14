package com.lightningkite.kiteui.lottie.models

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * Shape types in Lottie.
 */
object ShapeType {
    const val ELLIPSE = "el"
    const val FILL = "fl"
    const val GRADIENT_FILL = "gf"
    const val GRADIENT_STROKE = "gs"
    const val GROUP = "gr"
    const val MERGE = "mm"
    const val OFFSET_PATH = "op"
    const val PATH = "sh"
    const val POLYSTAR = "sr"
    const val RECTANGLE = "rc"
    const val REPEATER = "rp"
    const val ROUND_CORNERS = "rd"
    const val STROKE = "st"
    const val TRANSFORM = "tr"
    const val TRIM = "tm"
}

/**
 * Fill rule
 */
object FillRule {
    const val NON_ZERO = 1
    const val EVEN_ODD = 2
}

/**
 * Line cap styles
 */
object LineCap {
    const val BUTT = 1
    const val ROUND = 2
    const val SQUARE = 3
}

/**
 * Line join styles
 */
object LineJoin {
    const val MITER = 1
    const val ROUND = 2
    const val BEVEL = 3
}

/**
 * Gradient type
 */
object GradientType {
    const val LINEAR = 1
    const val RADIAL = 2
}

/**
 * Trim path modes
 */
object TrimMode {
    const val SIMULTANEOUSLY = 1
    const val INDIVIDUALLY = 2
}

/**
 * Base shape item.
 */
@Serializable(with = LottieShapeSerializer::class)
sealed class LottieShape {
    abstract val name: String?
    abstract val hidden: Boolean
    abstract val blendMode: Int
}

@Serializable
data class LottieShapeGroup(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val numProperties: Int? = null,
    val items: List<LottieShape> = emptyList(),
) : LottieShape()

@Serializable
data class LottieShapeEllipse(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val direction: Int = 1,
    val position: LottieAnimatedMultiValue? = null,
    val size: LottieAnimatedMultiValue? = null,
) : LottieShape()

@Serializable
data class LottieShapeRectangle(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val direction: Int = 1,
    val position: LottieAnimatedMultiValue? = null,
    val size: LottieAnimatedMultiValue? = null,
    val radius: LottieAnimatedValue? = null,
) : LottieShape()

@Serializable
data class LottieShapePath(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val direction: Int = 1,
    val path: LottieAnimatedShape? = null,
) : LottieShape()

@Serializable
data class LottieShapePolystar(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val direction: Int = 1,
    val starType: Int = 1,
    val position: LottieAnimatedMultiValue? = null,
    val points: LottieAnimatedValue? = null,
    val rotation: LottieAnimatedValue? = null,
    val outerRadius: LottieAnimatedValue? = null,
    val outerRoundness: LottieAnimatedValue? = null,
    val innerRadius: LottieAnimatedValue? = null,
    val innerRoundness: LottieAnimatedValue? = null,
) : LottieShape()

@Serializable
data class LottieShapeFill(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val color: LottieAnimatedMultiValue? = null,
    val opacity: LottieAnimatedValue? = null,
    val fillRule: Int = FillRule.NON_ZERO,
) : LottieShape()

@Serializable
data class LottieShapeStroke(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val color: LottieAnimatedMultiValue? = null,
    val opacity: LottieAnimatedValue? = null,
    val width: LottieAnimatedValue? = null,
    val lineCap: Int = LineCap.ROUND,
    val lineJoin: Int = LineJoin.ROUND,
    val miterLimit: Double = 4.0,
    val dashes: List<LottieDash> = emptyList(),
) : LottieShape()

@Serializable
data class LottieShapeGradientFill(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val gradientType: Int = GradientType.LINEAR,
    val startPoint: LottieAnimatedMultiValue? = null,
    val endPoint: LottieAnimatedMultiValue? = null,
    val colors: LottieGradientColors? = null,
    val opacity: LottieAnimatedValue? = null,
    val fillRule: Int = FillRule.NON_ZERO,
    val highlightLength: LottieAnimatedValue? = null,
    val highlightAngle: LottieAnimatedValue? = null,
) : LottieShape()

@Serializable
data class LottieShapeGradientStroke(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val gradientType: Int = GradientType.LINEAR,
    val startPoint: LottieAnimatedMultiValue? = null,
    val endPoint: LottieAnimatedMultiValue? = null,
    val colors: LottieGradientColors? = null,
    val opacity: LottieAnimatedValue? = null,
    val width: LottieAnimatedValue? = null,
    val lineCap: Int = LineCap.ROUND,
    val lineJoin: Int = LineJoin.ROUND,
    val miterLimit: Double = 4.0,
    val dashes: List<LottieDash> = emptyList(),
    val highlightLength: LottieAnimatedValue? = null,
    val highlightAngle: LottieAnimatedValue? = null,
) : LottieShape()

@Serializable
data class LottieShapeTransform(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val anchorPoint: LottieAnimatedMultiValue? = null,
    val position: LottieAnimatedMultiValue? = null,
    val scale: LottieAnimatedMultiValue? = null,
    val rotation: LottieAnimatedValue? = null,
    val opacity: LottieAnimatedValue? = null,
    val skew: LottieAnimatedValue? = null,
    val skewAxis: LottieAnimatedValue? = null,
) : LottieShape()

@Serializable
data class LottieShapeTrim(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val start: LottieAnimatedValue? = null,
    val end: LottieAnimatedValue? = null,
    val offset: LottieAnimatedValue? = null,
    val trimMode: Int = TrimMode.SIMULTANEOUSLY,
) : LottieShape()

@Serializable
data class LottieShapeRoundCorners(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val radius: LottieAnimatedValue? = null,
) : LottieShape()

@Serializable
data class LottieShapeMerge(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val mode: Int = 1,
) : LottieShape()

@Serializable
data class LottieShapeRepeater(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val copies: LottieAnimatedValue? = null,
    val offset: LottieAnimatedValue? = null,
    val composite: Int = 1,
    val transform: LottieRepeaterTransform? = null,
) : LottieShape()

@Serializable
data class LottieShapeUnknown(
    override val name: String? = null,
    override val hidden: Boolean = false,
    override val blendMode: Int = 0,
    val type: String = "",
) : LottieShape()

/**
 * Dash definition for strokes
 */
@Serializable
data class LottieDash(
    @SerialName("n") val type: String? = null,
    @SerialName("v") val value: LottieAnimatedValue? = null,
)

/**
 * Gradient colors container
 */
@Serializable
data class LottieGradientColors(
    @SerialName("p") val numColors: Int = 0,
    @SerialName("k") val colors: LottieAnimatedMultiValue? = null,
)

/**
 * Repeater transform
 */
@Serializable
data class LottieRepeaterTransform(
    @SerialName("a") val anchorPoint: LottieAnimatedMultiValue? = null,
    @SerialName("p") val position: LottieAnimatedMultiValue? = null,
    @SerialName("s") val scale: LottieAnimatedMultiValue? = null,
    @SerialName("r") val rotation: LottieAnimatedValue? = null,
    @SerialName("so") val startOpacity: LottieAnimatedValue? = null,
    @SerialName("eo") val endOpacity: LottieAnimatedValue? = null,
    @SerialName("sk") val skew: LottieAnimatedValue? = null,
    @SerialName("sa") val skewAxis: LottieAnimatedValue? = null,
)

/**
 * Custom serializer for LottieShape that handles polymorphic deserialization
 * based on the "ty" field.
 */
object LottieShapeSerializer : KSerializer<LottieShape> {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LottieShape")

    override fun serialize(encoder: Encoder, value: LottieShape) {
        throw NotImplementedError("Serialization not implemented")
    }

    override fun deserialize(decoder: Decoder): LottieShape {
        val input = decoder as JsonDecoder
        val obj = input.decodeJsonElement().jsonObject

        val type = obj["ty"]?.jsonPrimitive?.contentOrNull ?: return LottieShapeUnknown()
        val name = obj["nm"]?.jsonPrimitive?.contentOrNull
        val hidden = obj["hd"]?.jsonPrimitive?.booleanOrNull ?: false
        val blendMode = obj["bm"]?.jsonPrimitive?.intOrNull ?: 0

        return when (type) {
            ShapeType.GROUP -> LottieShapeGroup(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                numProperties = obj["np"]?.jsonPrimitive?.intOrNull,
                items = obj["it"]?.jsonArray?.map { json.decodeFromJsonElement(it) } ?: emptyList()
            )
            ShapeType.ELLIPSE -> LottieShapeEllipse(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                direction = obj["d"]?.jsonPrimitive?.intOrNull ?: 1,
                position = obj["p"]?.let { json.decodeFromJsonElement(it) },
                size = obj["s"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.RECTANGLE -> LottieShapeRectangle(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                direction = obj["d"]?.jsonPrimitive?.intOrNull ?: 1,
                position = obj["p"]?.let { json.decodeFromJsonElement(it) },
                size = obj["s"]?.let { json.decodeFromJsonElement(it) },
                radius = obj["r"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.PATH -> LottieShapePath(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                direction = obj["d"]?.jsonPrimitive?.intOrNull ?: 1,
                path = obj["ks"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.POLYSTAR -> LottieShapePolystar(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                direction = obj["d"]?.jsonPrimitive?.intOrNull ?: 1,
                starType = obj["sy"]?.jsonPrimitive?.intOrNull ?: 1,
                position = obj["p"]?.let { json.decodeFromJsonElement(it) },
                points = obj["pt"]?.let { json.decodeFromJsonElement(it) },
                rotation = obj["r"]?.let { json.decodeFromJsonElement(it) },
                outerRadius = obj["or"]?.let { json.decodeFromJsonElement(it) },
                outerRoundness = obj["os"]?.let { json.decodeFromJsonElement(it) },
                innerRadius = obj["ir"]?.let { json.decodeFromJsonElement(it) },
                innerRoundness = obj["is"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.FILL -> LottieShapeFill(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                color = obj["c"]?.let { json.decodeFromJsonElement(it) },
                opacity = obj["o"]?.let { json.decodeFromJsonElement(it) },
                fillRule = obj["r"]?.jsonPrimitive?.intOrNull ?: FillRule.NON_ZERO
            )
            ShapeType.STROKE -> LottieShapeStroke(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                color = obj["c"]?.let { json.decodeFromJsonElement(it) },
                opacity = obj["o"]?.let { json.decodeFromJsonElement(it) },
                width = obj["w"]?.let { json.decodeFromJsonElement(it) },
                lineCap = obj["lc"]?.jsonPrimitive?.intOrNull ?: LineCap.ROUND,
                lineJoin = obj["lj"]?.jsonPrimitive?.intOrNull ?: LineJoin.ROUND,
                miterLimit = obj["ml"]?.jsonPrimitive?.doubleOrNull ?: 4.0,
                dashes = obj["d"]?.jsonArray?.map { json.decodeFromJsonElement(it) } ?: emptyList()
            )
            ShapeType.GRADIENT_FILL -> LottieShapeGradientFill(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                gradientType = obj["t"]?.jsonPrimitive?.intOrNull ?: GradientType.LINEAR,
                startPoint = obj["s"]?.let { json.decodeFromJsonElement(it) },
                endPoint = obj["e"]?.let { json.decodeFromJsonElement(it) },
                colors = obj["g"]?.let { json.decodeFromJsonElement(it) },
                opacity = obj["o"]?.let { json.decodeFromJsonElement(it) },
                fillRule = obj["r"]?.jsonPrimitive?.intOrNull ?: FillRule.NON_ZERO,
                highlightLength = obj["h"]?.let { json.decodeFromJsonElement(it) },
                highlightAngle = obj["a"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.GRADIENT_STROKE -> LottieShapeGradientStroke(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                gradientType = obj["t"]?.jsonPrimitive?.intOrNull ?: GradientType.LINEAR,
                startPoint = obj["s"]?.let { json.decodeFromJsonElement(it) },
                endPoint = obj["e"]?.let { json.decodeFromJsonElement(it) },
                colors = obj["g"]?.let { json.decodeFromJsonElement(it) },
                opacity = obj["o"]?.let { json.decodeFromJsonElement(it) },
                width = obj["w"]?.let { json.decodeFromJsonElement(it) },
                lineCap = obj["lc"]?.jsonPrimitive?.intOrNull ?: LineCap.ROUND,
                lineJoin = obj["lj"]?.jsonPrimitive?.intOrNull ?: LineJoin.ROUND,
                miterLimit = obj["ml"]?.jsonPrimitive?.doubleOrNull ?: 4.0,
                dashes = obj["d"]?.jsonArray?.map { json.decodeFromJsonElement(it) } ?: emptyList(),
                highlightLength = obj["h"]?.let { json.decodeFromJsonElement(it) },
                highlightAngle = obj["a"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.TRANSFORM -> LottieShapeTransform(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                anchorPoint = obj["a"]?.let { json.decodeFromJsonElement(it) },
                position = obj["p"]?.let { json.decodeFromJsonElement(it) },
                scale = obj["s"]?.let { json.decodeFromJsonElement(it) },
                rotation = obj["r"]?.let { json.decodeFromJsonElement(it) },
                opacity = obj["o"]?.let { json.decodeFromJsonElement(it) },
                skew = obj["sk"]?.let { json.decodeFromJsonElement(it) },
                skewAxis = obj["sa"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.TRIM -> LottieShapeTrim(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                start = obj["s"]?.let { json.decodeFromJsonElement(it) },
                end = obj["e"]?.let { json.decodeFromJsonElement(it) },
                offset = obj["o"]?.let { json.decodeFromJsonElement(it) },
                trimMode = obj["m"]?.jsonPrimitive?.intOrNull ?: TrimMode.SIMULTANEOUSLY
            )
            ShapeType.ROUND_CORNERS -> LottieShapeRoundCorners(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                radius = obj["r"]?.let { json.decodeFromJsonElement(it) }
            )
            ShapeType.MERGE -> LottieShapeMerge(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                mode = obj["mm"]?.jsonPrimitive?.intOrNull ?: 1
            )
            ShapeType.REPEATER -> LottieShapeRepeater(
                name = name,
                hidden = hidden,
                blendMode = blendMode,
                copies = obj["c"]?.let { json.decodeFromJsonElement(it) },
                offset = obj["o"]?.let { json.decodeFromJsonElement(it) },
                composite = obj["m"]?.jsonPrimitive?.intOrNull ?: 1,
                transform = obj["tr"]?.let { json.decodeFromJsonElement(it) }
            )
            else -> LottieShapeUnknown(name = name, hidden = hidden, blendMode = blendMode, type = type)
        }
    }
}
