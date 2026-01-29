package com.lightningkite.kiteui.lottie.models

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Serializer for LottieAnimatedValue.
 * Handles both static values and keyframe arrays.
 */
object LottieAnimatedValueSerializer : KSerializer<LottieAnimatedValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LottieAnimatedValue")

    override fun serialize(encoder: Encoder, value: LottieAnimatedValue) {
        // For now, we only need deserialization
        throw NotImplementedError("Serialization not implemented")
    }

    override fun deserialize(decoder: Decoder): LottieAnimatedValue {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()

        return when (element) {
            is JsonObject -> parseAnimatedValue(element)
            is JsonPrimitive -> LottieAnimatedValue(
                animated = false,
                staticValue = element.doubleOrNull ?: 0.0
            )
            is JsonArray -> {
                // Could be static array value or keyframes
                if (element.isEmpty()) {
                    LottieAnimatedValue(animated = false, staticValue = 0.0)
                } else {
                    val first = element[0]
                    if (first is JsonObject && first.containsKey("t")) {
                        // Keyframes array
                        val keyframes = element.map { json.decodeFromJsonElement<LottieKeyframe>(it) }
                        LottieAnimatedValue(animated = true, keyframes = keyframes)
                    } else {
                        // Static value as array (take first element)
                        LottieAnimatedValue(
                            animated = false,
                            staticValue = (first as? JsonPrimitive)?.doubleOrNull ?: 0.0
                        )
                    }
                }
            }
            else -> LottieAnimatedValue()
        }
    }

    private fun parseAnimatedValue(obj: JsonObject): LottieAnimatedValue {
        val animated = (obj["a"] as? JsonPrimitive)?.intOrNull == 1
        val expression = (obj["x"] as? JsonPrimitive)?.contentOrNull

        val k = obj["k"]
        return when {
            k == null -> LottieAnimatedValue(animated = false, expression = expression)
            !animated && k is JsonPrimitive -> LottieAnimatedValue(
                animated = false,
                staticValue = k.doubleOrNull ?: 0.0,
                expression = expression
            )
            !animated && k is JsonArray -> {
                // Static value as array
                val value = (k.firstOrNull() as? JsonPrimitive)?.doubleOrNull ?: 0.0
                LottieAnimatedValue(animated = false, staticValue = value, expression = expression)
            }
            k is JsonArray -> {
                // Keyframes
                val keyframes = k.mapNotNull {
                    try { json.decodeFromJsonElement<LottieKeyframe>(it) } catch (e: Exception) { null }
                }
                LottieAnimatedValue(animated = true, keyframes = keyframes, expression = expression)
            }
            else -> LottieAnimatedValue(expression = expression)
        }
    }
}

/**
 * Serializer for LottieAnimatedMultiValue.
 * Handles multi-dimensional values (points, scales, colors).
 */
object LottieAnimatedMultiValueSerializer : KSerializer<LottieAnimatedMultiValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LottieAnimatedMultiValue")

    override fun serialize(encoder: Encoder, value: LottieAnimatedMultiValue) {
        throw NotImplementedError("Serialization not implemented")
    }

    override fun deserialize(decoder: Decoder): LottieAnimatedMultiValue {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()

        return when (element) {
            is JsonObject -> parseAnimatedMultiValue(element)
            is JsonArray -> {
                // Could be static array or keyframes
                if (element.isEmpty()) {
                    LottieAnimatedMultiValue()
                } else {
                    val first = element[0]
                    if (first is JsonObject && first.containsKey("t")) {
                        // Keyframes
                        val keyframes = element.map { json.decodeFromJsonElement<LottieMultiKeyframe>(it) }
                        LottieAnimatedMultiValue(animated = true, keyframes = keyframes)
                    } else {
                        // Static multi-value
                        val values = element.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
                        LottieAnimatedMultiValue(animated = false, staticValue = values)
                    }
                }
            }
            else -> LottieAnimatedMultiValue()
        }
    }

    private fun parseAnimatedMultiValue(obj: JsonObject): LottieAnimatedMultiValue {
        val animated = (obj["a"] as? JsonPrimitive)?.intOrNull == 1
        val expression = (obj["x"] as? JsonPrimitive)?.contentOrNull

        val k = obj["k"]
        return when {
            k == null -> LottieAnimatedMultiValue(expression = expression)
            !animated && k is JsonArray -> {
                val first = k.firstOrNull()
                if (first is JsonObject && first.containsKey("t")) {
                    // Actually animated keyframes
                    val keyframes = k.map { json.decodeFromJsonElement<LottieMultiKeyframe>(it) }
                    LottieAnimatedMultiValue(animated = true, keyframes = keyframes, expression = expression)
                } else {
                    // Static multi-value
                    val values = k.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
                    LottieAnimatedMultiValue(animated = false, staticValue = values, expression = expression)
                }
            }
            k is JsonArray -> {
                val first = k.firstOrNull()
                if (first is JsonObject && first.containsKey("t")) {
                    // Keyframes
                    val keyframes = k.map { json.decodeFromJsonElement<LottieMultiKeyframe>(it) }
                    LottieAnimatedMultiValue(animated = true, keyframes = keyframes, expression = expression)
                } else {
                    // Static multi-value
                    val values = k.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
                    LottieAnimatedMultiValue(animated = false, staticValue = values, expression = expression)
                }
            }
            else -> LottieAnimatedMultiValue(expression = expression)
        }
    }
}

/**
 * Serializer for LottieAnimatedShape.
 * Handles animated bezier path data.
 */
object LottieAnimatedShapeSerializer : KSerializer<LottieAnimatedShape> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LottieAnimatedShape")

    override fun serialize(encoder: Encoder, value: LottieAnimatedShape) {
        throw NotImplementedError("Serialization not implemented")
    }

    override fun deserialize(decoder: Decoder): LottieAnimatedShape {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()

        return when (element) {
            is JsonObject -> parseAnimatedShape(element)
            else -> LottieAnimatedShape()
        }
    }

    private fun parseAnimatedShape(obj: JsonObject): LottieAnimatedShape {
        val animated = (obj["a"] as? JsonPrimitive)?.intOrNull == 1

        val k = obj["k"]
        return when {
            k == null -> LottieAnimatedShape()
            !animated && k is JsonObject -> {
                // Static shape
                val shape = json.decodeFromJsonElement<LottieBezierPath>(k)
                LottieAnimatedShape(animated = false, staticValue = shape)
            }
            k is JsonArray -> {
                val first = k.firstOrNull()
                if (first is JsonObject && first.containsKey("t")) {
                    // Keyframes
                    val keyframes = k.map { json.decodeFromJsonElement<LottieShapeKeyframe>(it) }
                    LottieAnimatedShape(animated = true, keyframes = keyframes)
                } else if (first is JsonObject) {
                    // Single static shape in array
                    val shape = json.decodeFromJsonElement<LottieBezierPath>(first)
                    LottieAnimatedShape(animated = false, staticValue = shape)
                } else {
                    LottieAnimatedShape()
                }
            }
            k is JsonObject -> {
                val shape = json.decodeFromJsonElement<LottieBezierPath>(k)
                LottieAnimatedShape(animated = false, staticValue = shape)
            }
            else -> LottieAnimatedShape()
        }
    }
}
