package com.lightningkite.kiteui.map.style

import com.lightningkite.kiteui.models.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmName

/**
 * A MapLibre expression that evaluates to a [T]. Constants are expressions too; create them with
 * [const].
 *
 * See https://maplibre.org/maplibre-style-spec/expressions/
 */
class Expression<out T> internal constructor(val json: JsonElement) {
    override fun equals(other: Any?) = other is Expression<*> && json == other.json
    override fun hashCode() = json.hashCode()
    override fun toString() = json.toString()
}

/**
 * An escape hatch for expressions this DSL doesn't cover. [json] is used as-is, so make sure it
 * actually evaluates to a [T].
 */
fun <T> rawExpression(json: JsonElement): Expression<T> = Expression(json)

private fun <T> op(name: String, vararg args: Any): Expression<T> = Expression(JsonArray(listOf(JsonPrimitive(name)) + args.map {
    when (it) {
        is Expression<*> -> it.json
        is JsonElement -> it
        is String -> JsonPrimitive(it)
        is Number -> JsonPrimitive(it)
        is Boolean -> JsonPrimitive(it)
        else -> error("Unsupported expression argument $it")
    }
}))

/** Arrays must be wrapped in `literal`, or MapLibre would read them as expressions. */
private fun <T> literalArray(items: List<JsonElement>): Expression<T> = op("literal", JsonArray(items))

// Constants

fun const(value: Double): Expression<Double> = Expression(JsonPrimitive(value))
fun const(value: Int): Expression<Double> = Expression(JsonPrimitive(value))
fun const(value: Boolean): Expression<Boolean> = Expression(JsonPrimitive(value))
fun const(value: String): Expression<String> = Expression(JsonPrimitive(value))
fun const(value: Color): Expression<Color> = Expression(JsonPrimitive(value.toWeb()))
fun <E : StyleEnum> const(value: E): Expression<E> = Expression(value.json)
fun const(value: Offset): Expression<Offset> = literalArray(listOf(JsonPrimitive(value.x), JsonPrimitive(value.y)))
fun const(value: Padding): Expression<Padding> =
    literalArray(listOf(value.top, value.right, value.bottom, value.left).map(::JsonPrimitive))

@JvmName("constDoubles")
fun const(value: List<Double>): Expression<List<Double>> = literalArray(value.map(::JsonPrimitive))

@JvmName("constStrings")
fun const(value: List<String>): Expression<List<String>> = literalArray(value.map(::JsonPrimitive))

@JvmName("constEnums")
fun <E : StyleEnum> const(value: List<E>): Expression<List<E>> = literalArray(value.map { it.json })

@JvmName("constAnchorOffsets")
fun const(value: List<SymbolLayer.AnchorOffset>): Expression<List<SymbolLayer.AnchorOffset>> = literalArray(value.flatMap {
    listOf(it.anchor.json, JsonArray(listOf(JsonPrimitive(it.offset.x), JsonPrimitive(it.offset.y))))
})

// Feature data and camera

/** The feature property [key]. Unchecked: if the property isn't a [T], use [asNumber] and friends instead. */
fun <T> get(key: String): Expression<T> = op("get", key)
fun has(key: String): Expression<Boolean> = op("has", key)

/** The feature's `id`. */
fun <T> featureId(): Expression<T> = op("id")

/** The feature's state value [key]. Only usable in paint properties that support it. */
fun <T> featureState(key: String): Expression<T> = op("feature-state", key)

/** `"Point"`, `"LineString"`, or `"Polygon"` (or their `Multi` versions). */
fun geometryType(): Expression<String> = op("geometry-type")

fun zoom(): Expression<Double> = op("zoom")

/** Progress along the line, from 0 to 1. Only usable in `LineLayer.gradient`. */
fun lineProgress(): Expression<Double> = op("line-progress")

// Types

/** Asserts that this is a number, falling back to [fallbacks] in order, and erroring if none are. */
fun Expression<*>.asNumber(vararg fallbacks: Expression<*>): Expression<Double> = op("number", this, *fallbacks)
fun Expression<*>.asString(vararg fallbacks: Expression<*>): Expression<String> = op("string", this, *fallbacks)
fun Expression<*>.asBoolean(vararg fallbacks: Expression<*>): Expression<Boolean> = op("boolean", this, *fallbacks)

/** Converts this to a number, falling back to [fallbacks] in order, and erroring if none convert. */
fun Expression<*>.toNumber(vararg fallbacks: Expression<*>): Expression<Double> = op("to-number", this, *fallbacks)
fun Expression<*>.toColor(vararg fallbacks: Expression<*>): Expression<Color> = op("to-color", this, *fallbacks)
fun Expression<*>.toBoolean(): Expression<Boolean> = op("to-boolean", this)
fun Expression<*>.convertToString(): Expression<String> = op("to-string", this)

// Formatted text and images

/** Plain text for text fields. MapLibre converts strings to formatted text on its own. */
fun Expression<String>.formatted(): Expression<Formatted> = Expression(json)
fun formatted(text: String): Expression<Formatted> = const(text).formatted()

/** A section of [format]ted text. Unset options use the layer's. */
class FormatSection(
    val text: Expression<String>,
    val fontScale: Expression<Double>? = null,
    val textFont: Expression<List<String>>? = null,
    val textColor: Expression<Color>? = null,
)

fun format(vararg sections: FormatSection): Expression<Formatted> = op("format", *sections.flatMap { section ->
    listOf(section.text.json, JsonObject(listOfNotNull(
        section.fontScale?.let { "font-scale" to it.json },
        section.textFont?.let { "text-font" to it.json },
        section.textColor?.let { "text-color" to it.json },
    ).toMap()))
}.toTypedArray())

fun image(name: String): Expression<ResolvedImage> = op("image", name)
fun image(name: Expression<String>): Expression<ResolvedImage> = op("image", name)

// Logic and comparison

infix fun <T> Expression<T>.eq(other: Expression<T>): Expression<Boolean> = op("==", this, other)
infix fun Expression<String>.eq(other: String): Expression<Boolean> = op("==", this, other)
infix fun Expression<Double>.eq(other: Double): Expression<Boolean> = op("==", this, other)
infix fun Expression<Boolean>.eq(other: Boolean): Expression<Boolean> = op("==", this, other)
infix fun <T> Expression<T>.neq(other: Expression<T>): Expression<Boolean> = op("!=", this, other)
infix fun Expression<String>.neq(other: String): Expression<Boolean> = op("!=", this, other)
infix fun Expression<Double>.neq(other: Double): Expression<Boolean> = op("!=", this, other)
infix fun Expression<Boolean>.neq(other: Boolean): Expression<Boolean> = op("!=", this, other)
infix fun Expression<Double>.lt(other: Expression<Double>): Expression<Boolean> = op("<", this, other)
infix fun Expression<Double>.lt(other: Double): Expression<Boolean> = op("<", this, other)
infix fun Expression<Double>.lte(other: Expression<Double>): Expression<Boolean> = op("<=", this, other)
infix fun Expression<Double>.lte(other: Double): Expression<Boolean> = op("<=", this, other)
infix fun Expression<Double>.gt(other: Expression<Double>): Expression<Boolean> = op(">", this, other)
infix fun Expression<Double>.gt(other: Double): Expression<Boolean> = op(">", this, other)
infix fun Expression<Double>.gte(other: Expression<Double>): Expression<Boolean> = op(">=", this, other)
infix fun Expression<Double>.gte(other: Double): Expression<Boolean> = op(">=", this, other)

fun all(vararg conditions: Expression<Boolean>): Expression<Boolean> = op("all", *conditions)
fun any(vararg conditions: Expression<Boolean>): Expression<Boolean> = op("any", *conditions)
operator fun Expression<Boolean>.not(): Expression<Boolean> = op("!", this)

/** The first value whose condition is true, or [fallback]. */
fun <T> case(vararg branches: Pair<Expression<Boolean>, Expression<T>>, fallback: Expression<T>): Expression<T> =
    op("case", *branches.flatMap { listOf(it.first, it.second) }.toTypedArray(), fallback)

/** The value for the label equal to [input], or [fallback]. */
fun <T> match(input: Expression<String>, vararg branches: Pair<String, Expression<T>>, fallback: Expression<T>): Expression<T> =
    op("match", input, *branches.flatMap { listOf(it.first, it.second) }.toTypedArray(), fallback)

@JvmName("matchNumber")
fun <T> match(input: Expression<Double>, vararg branches: Pair<Double, Expression<T>>, fallback: Expression<T>): Expression<T> =
    op("match", input, *branches.flatMap { listOf(it.first, it.second) }.toTypedArray(), fallback)

/** The first value that isn't null. */
fun <T> coalesce(vararg values: Expression<T>): Expression<T> = op("coalesce", *values)

// Ramps

sealed interface Interpolation {
    data object Linear : Interpolation
    data class Exponential(val base: Double) : Interpolation
    data class CubicBezier(val x1: Double, val y1: Double, val x2: Double, val y2: Double) : Interpolation
}

private fun Interpolation.toJson(): JsonElement = when (this) {
    Interpolation.Linear -> op<Nothing>("linear").json
    is Interpolation.Exponential -> op<Nothing>("exponential", base).json
    is Interpolation.CubicBezier -> op<Nothing>("cubic-bezier", x1, y1, x2, y2).json
}

/**
 * Smoothly interpolates between [stops] (input to output, in ascending input order) based on
 * [input]. Only numbers, colors, offsets, paddings, and number arrays can be interpolated.
 */
fun <T> interpolate(
    input: Expression<Double>,
    vararg stops: Pair<Double, Expression<T>>,
    interpolation: Interpolation = Interpolation.Linear,
): Expression<T> = op("interpolate", interpolation.toJson(), input, *stops.flatMap { listOf(it.first, it.second) }.toTypedArray())

/** [base] below the first stop, then the value of the highest stop (in ascending order) at or below [input]. */
fun <T> step(input: Expression<Double>, base: Expression<T>, vararg stops: Pair<Double, Expression<T>>): Expression<T> =
    op("step", input, base, *stops.flatMap { listOf(it.first, it.second) }.toTypedArray())

// Math and strings

operator fun Expression<Double>.plus(other: Expression<Double>): Expression<Double> = op("+", this, other)
operator fun Expression<Double>.plus(other: Double): Expression<Double> = op("+", this, other)
operator fun Expression<Double>.minus(other: Expression<Double>): Expression<Double> = op("-", this, other)
operator fun Expression<Double>.minus(other: Double): Expression<Double> = op("-", this, other)
operator fun Expression<Double>.times(other: Expression<Double>): Expression<Double> = op("*", this, other)
operator fun Expression<Double>.times(other: Double): Expression<Double> = op("*", this, other)
operator fun Expression<Double>.div(other: Expression<Double>): Expression<Double> = op("/", this, other)
operator fun Expression<Double>.div(other: Double): Expression<Double> = op("/", this, other)
operator fun Expression<Double>.rem(other: Expression<Double>): Expression<Double> = op("%", this, other)
operator fun Expression<Double>.rem(other: Double): Expression<Double> = op("%", this, other)
fun min(vararg values: Expression<Double>): Expression<Double> = op("min", *values)
fun max(vararg values: Expression<Double>): Expression<Double> = op("max", *values)

fun concat(vararg values: Expression<*>): Expression<String> = op("concat", *values)
fun Expression<String>.uppercase(): Expression<String> = op("upcase", this)
fun Expression<String>.lowercase(): Expression<String> = op("downcase", this)
