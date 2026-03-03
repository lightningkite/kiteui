package com.lightningkite.kiteui.lottie

import com.lightningkite.kiteui.models.Color
import kotlinx.serialization.json.*

/**
 * Represents a color found in a Lottie animation along with metadata
 * about where it appears in the animation structure.
 */
data class LottieColor(
    val color: Color,
    val layerName: String?,
    val layerIndex: Int?,
    val shapeName: String?,
    /** The type of color source: "fl" (fill), "st" (stroke), "gf" (gradient fill), "gs" (gradient stroke), "solid" (solid layer), "text" */
    val shapeType: String,
)

private val lottieJson = Json { isLenient = true }

/**
 * Context tracked while traversing the Lottie JSON tree.
 */
private data class ColorContext(
    val layerName: String? = null,
    val layerIndex: Int? = null,
)

/**
 * Apply a color transform to all colors in a Lottie JSON string.
 * The transform receives a [LottieColor] with metadata about where the color
 * appears in the animation, and returns the replacement [Color].
 */
fun applyColorTransform(json: String, transform: (LottieColor) -> Color): String {
    val root = lottieJson.parseToJsonElement(json)
    val transformed = transformElement(root, transform, ColorContext())
    return transformed.toString()
}

/**
 * Extract all colors from a Lottie JSON string with metadata about
 * where each color appears in the animation.
 */
fun extractLottieColors(json: String): List<LottieColor> {
    val colors = mutableListOf<LottieColor>()
    val root = lottieJson.parseToJsonElement(json)
    collectColors(root, colors, ColorContext())
    return colors
}

// --- JSON tree walking (transform) ---

private fun transformElement(element: JsonElement, transform: (LottieColor) -> Color, ctx: ColorContext): JsonElement =
    when (element) {
        is JsonObject -> transformObject(element, transform, ctx)
        is JsonArray -> JsonArray(element.map { transformElement(it, transform, ctx) })
        is JsonPrimitive -> element
    }

private fun transformObject(obj: JsonObject, transform: (LottieColor) -> Color, ctx: ColorContext): JsonObject {
    val type = obj["ty"]?.jsonPrimitive?.contentOrNull
    val typeInt = obj["ty"]?.jsonPrimitive?.intOrNull

    // Detect layer context: layers have "ind" (index) field
    val effectiveCtx = if (obj.containsKey("ind")) {
        ColorContext(
            layerName = obj["nm"]?.jsonPrimitive?.contentOrNull,
            layerIndex = obj["ind"]?.jsonPrimitive?.intOrNull,
        )
    } else ctx

    val shapeName = obj["nm"]?.jsonPrimitive?.contentOrNull

    val result = buildMap {
        for ((key, value) in obj) {
            put(
                key, when {
                    // Shape fill or stroke color
                    key == "c" && type in COLOR_SHAPE_TYPES ->
                        transformAnimatedColor(value, transform, effectiveCtx, shapeName, type!!)
                    // Solid layer color
                    key == "sc" && typeInt == 1 ->
                        transformHexColor(value, transform, effectiveCtx)
                    // Gradient colors
                    key == "g" && type in GRADIENT_SHAPE_TYPES ->
                        transformGradientColors(value, transform, effectiveCtx, shapeName, type!!)
                    // Text document fill/stroke color
                    key == "fc" && isTextContext(obj) ->
                        transformColorArray(value, transform, effectiveCtx, shapeName, "text")

                    key == "sc" && isTextContext(obj) ->
                        transformColorArray(value, transform, effectiveCtx, shapeName, "text")
                    // Recurse into everything else
                    else -> transformElement(value, transform, effectiveCtx)
                }
            )
        }
    }
    return JsonObject(result)
}

private val COLOR_SHAPE_TYPES = setOf("fl", "st")
private val GRADIENT_SHAPE_TYPES = setOf("gf", "gs")

private fun transformAnimatedColor(
    element: JsonElement, transform: (LottieColor) -> Color,
    ctx: ColorContext, shapeName: String?, shapeType: String
): JsonElement {
    if (element !is JsonObject) return element
    val k = element["k"] ?: return element
    val animated = element["a"]?.jsonPrimitive?.intOrNull ?: 0

    val newK = if (animated == 0 && k is JsonArray && k.firstOrNull() is JsonPrimitive) {
        transformColorArray(k, transform, ctx, shapeName, shapeType)
    } else if (k is JsonArray && k.firstOrNull() is JsonObject) {
        JsonArray(k.map { keyframe ->
            if (keyframe is JsonObject) {
                val result = buildMap {
                    for ((key, value) in keyframe) {
                        put(
                            key, when (key) {
                                "s", "e" -> transformColorArray(value, transform, ctx, shapeName, shapeType)
                                else -> value
                            }
                        )
                    }
                }
                JsonObject(result)
            } else keyframe
        })
    } else k

    return JsonObject(element.toMutableMap().apply { put("k", newK) })
}

private fun transformColorArray(
    element: JsonElement, transform: (LottieColor) -> Color,
    ctx: ColorContext, shapeName: String?, shapeType: String
): JsonElement {
    if (element !is JsonArray) return element
    if (element.size < 3) return element
    val first = element.firstOrNull()
    if (first !is JsonPrimitive || !first.isString && first.doubleOrNull == null) return element

    val r = element[0].jsonPrimitive.doubleOrNull ?: return element
    val g = element[1].jsonPrimitive.doubleOrNull ?: return element
    val b = element[2].jsonPrimitive.doubleOrNull ?: return element
    val a = if (element.size > 3) element[3].jsonPrimitive.doubleOrNull ?: 1.0 else 1.0

    val color = Color(alpha = a.toFloat(), red = r.toFloat(), green = g.toFloat(), blue = b.toFloat())
    val lottieColor = LottieColor(color, ctx.layerName, ctx.layerIndex, shapeName, shapeType)
    val transformed = transform(lottieColor)

    return if (element.size > 3) {
        JsonArray(
            listOf(
                JsonPrimitive(transformed.red.toDouble()),
                JsonPrimitive(transformed.green.toDouble()),
                JsonPrimitive(transformed.blue.toDouble()),
                JsonPrimitive(transformed.alpha.toDouble()),
            )
        )
    } else {
        JsonArray(
            listOf(
                JsonPrimitive(transformed.red.toDouble()),
                JsonPrimitive(transformed.green.toDouble()),
                JsonPrimitive(transformed.blue.toDouble()),
            )
        )
    }
}

private fun transformHexColor(
    element: JsonElement, transform: (LottieColor) -> Color, ctx: ColorContext
): JsonElement {
    if (element !is JsonPrimitive || !element.isString) return element
    val hex = element.content
    if (!hex.startsWith("#")) return element
    val color = Color.fromHexString(hex)
    val lottieColor = LottieColor(color, ctx.layerName, ctx.layerIndex, ctx.layerName, "solid")
    val transformed = transform(lottieColor)
    return JsonPrimitive(transformed.toAlphalessWeb())
}

private fun transformGradientColors(
    element: JsonElement, transform: (LottieColor) -> Color,
    ctx: ColorContext, shapeName: String?, shapeType: String
): JsonElement {
    if (element !is JsonObject) return element
    val numColors = element["p"]?.jsonPrimitive?.intOrNull ?: return element
    val k = element["k"] ?: return element

    val newK = transformAnimatedGradient(k, numColors, transform, ctx, shapeName, shapeType)
    return JsonObject(element.toMutableMap().apply { put("k", newK) })
}

private fun transformAnimatedGradient(
    element: JsonElement, numColors: Int, transform: (LottieColor) -> Color,
    ctx: ColorContext, shapeName: String?, shapeType: String
): JsonElement {
    if (element !is JsonObject) return element
    val k = element["k"] ?: return element
    val animated = element["a"]?.jsonPrimitive?.intOrNull ?: 0

    val newK = if (animated == 0 && k is JsonArray && k.firstOrNull() is JsonPrimitive) {
        transformGradientStops(k, numColors, transform, ctx, shapeName, shapeType)
    } else if (k is JsonArray && k.firstOrNull() is JsonObject) {
        JsonArray(k.map { keyframe ->
            if (keyframe is JsonObject) {
                val result = buildMap {
                    for ((key, value) in keyframe) {
                        put(
                            key, when (key) {
                                "s", "e" -> if (value is JsonArray) transformGradientStops(
                                    value,
                                    numColors,
                                    transform,
                                    ctx,
                                    shapeName,
                                    shapeType
                                ) else value

                                else -> value
                            }
                        )
                    }
                }
                JsonObject(result)
            } else keyframe
        })
    } else k

    return JsonObject(element.toMutableMap().apply { put("k", newK) })
}

private fun transformGradientStops(
    array: JsonArray, numColors: Int, transform: (LottieColor) -> Color,
    ctx: ColorContext, shapeName: String?, shapeType: String
): JsonArray {
    val values = array.map { it.jsonPrimitive.doubleOrNull ?: 0.0 }.toMutableList()

    for (i in 0 until numColors) {
        val offset = i * 4
        if (offset + 3 >= values.size) break
        val r = values[offset + 1].toFloat()
        val g = values[offset + 2].toFloat()
        val b = values[offset + 3].toFloat()
        val color = Color(alpha = 1f, red = r, green = g, blue = b)
        val lottieColor = LottieColor(color, ctx.layerName, ctx.layerIndex, shapeName, shapeType)
        val transformed = transform(lottieColor)
        values[offset + 1] = transformed.red.toDouble()
        values[offset + 2] = transformed.green.toDouble()
        values[offset + 3] = transformed.blue.toDouble()
    }

    return JsonArray(values.map { JsonPrimitive(it) })
}

private fun isTextContext(obj: JsonObject): Boolean {
    return obj.containsKey("f") && obj.containsKey("s") && (obj.containsKey("fc") || obj.containsKey("sc"))
}

// --- Color extraction ---

private fun collectColors(element: JsonElement, colors: MutableList<LottieColor>, ctx: ColorContext) {
    when (element) {
        is JsonObject -> collectColorsFromObject(element, colors, ctx)
        is JsonArray -> element.forEach { collectColors(it, colors, ctx) }
        is JsonPrimitive -> {}
    }
}

private fun collectColorsFromObject(obj: JsonObject, colors: MutableList<LottieColor>, ctx: ColorContext) {
    val type = obj["ty"]?.jsonPrimitive?.contentOrNull
    val typeInt = obj["ty"]?.jsonPrimitive?.intOrNull

    val effectiveCtx = if (obj.containsKey("ind")) {
        ColorContext(
            layerName = obj["nm"]?.jsonPrimitive?.contentOrNull,
            layerIndex = obj["ind"]?.jsonPrimitive?.intOrNull,
        )
    } else ctx

    val shapeName = obj["nm"]?.jsonPrimitive?.contentOrNull

    for ((key, value) in obj) {
        when {
            key == "c" && type in COLOR_SHAPE_TYPES ->
                collectAnimatedColor(value, colors, effectiveCtx, shapeName, type!!)

            key == "sc" && typeInt == 1 -> {
                if (value is JsonPrimitive && value.isString && value.content.startsWith("#")) {
                    colors.add(
                        LottieColor(
                            Color.fromHexString(value.content),
                            effectiveCtx.layerName, effectiveCtx.layerIndex,
                            effectiveCtx.layerName, "solid"
                        )
                    )
                }
            }

            key == "g" && type in GRADIENT_SHAPE_TYPES ->
                collectGradientColors(value, colors, effectiveCtx, shapeName, type!!)

            key == "fc" && isTextContext(obj) ->
                collectColorArray(value, colors, effectiveCtx, shapeName, "text")

            key == "sc" && isTextContext(obj) ->
                collectColorArray(value, colors, effectiveCtx, shapeName, "text")

            else -> collectColors(value, colors, effectiveCtx)
        }
    }
}

private fun collectAnimatedColor(
    element: JsonElement, colors: MutableList<LottieColor>,
    ctx: ColorContext, shapeName: String?, shapeType: String
) {
    if (element !is JsonObject) return
    val k = element["k"] ?: return
    val animated = element["a"]?.jsonPrimitive?.intOrNull ?: 0

    if (animated == 0 && k is JsonArray && k.firstOrNull() is JsonPrimitive) {
        collectColorArray(k, colors, ctx, shapeName, shapeType)
    } else if (k is JsonArray && k.firstOrNull() is JsonObject) {
        for (keyframe in k) {
            if (keyframe is JsonObject) {
                keyframe["s"]?.let { collectColorArray(it, colors, ctx, shapeName, shapeType) }
                keyframe["e"]?.let { collectColorArray(it, colors, ctx, shapeName, shapeType) }
            }
        }
    }
}

private fun collectColorArray(
    element: JsonElement, colors: MutableList<LottieColor>,
    ctx: ColorContext, shapeName: String?, shapeType: String
) {
    if (element !is JsonArray || element.size < 3) return
    val r = element[0].jsonPrimitive.doubleOrNull ?: return
    val g = element[1].jsonPrimitive.doubleOrNull ?: return
    val b = element[2].jsonPrimitive.doubleOrNull ?: return
    val a = if (element.size > 3) element[3].jsonPrimitive.doubleOrNull ?: 1.0 else 1.0
    colors.add(
        LottieColor(
            Color(alpha = a.toFloat(), red = r.toFloat(), green = g.toFloat(), blue = b.toFloat()),
            ctx.layerName, ctx.layerIndex, shapeName, shapeType
        )
    )
}

private fun collectGradientColors(
    element: JsonElement, colors: MutableList<LottieColor>,
    ctx: ColorContext, shapeName: String?, shapeType: String
) {
    if (element !is JsonObject) return
    val numColors = element["p"]?.jsonPrimitive?.intOrNull ?: return
    val k = element["k"] ?: return
    if (k !is JsonObject) return
    val animated = k["a"]?.jsonPrimitive?.intOrNull ?: 0
    val kValue = k["k"] ?: return

    fun collectFromStops(array: JsonArray) {
        val values = array.mapNotNull { it.jsonPrimitive.doubleOrNull }
        for (i in 0 until numColors) {
            val offset = i * 4
            if (offset + 3 >= values.size) break
            colors.add(
                LottieColor(
                    Color(
                        alpha = 1f,
                        red = values[offset + 1].toFloat(),
                        green = values[offset + 2].toFloat(),
                        blue = values[offset + 3].toFloat(),
                    ),
                    ctx.layerName, ctx.layerIndex, shapeName, shapeType
                )
            )
        }
    }

    if (animated == 0 && kValue is JsonArray && kValue.firstOrNull() is JsonPrimitive) {
        collectFromStops(kValue)
    } else if (kValue is JsonArray && kValue.firstOrNull() is JsonObject) {
        for (keyframe in kValue) {
            if (keyframe is JsonObject) {
                (keyframe["s"] as? JsonArray)?.let { collectFromStops(it) }
                (keyframe["e"] as? JsonArray)?.let { collectFromStops(it) }
            }
        }
    }
}
