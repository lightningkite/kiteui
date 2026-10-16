package com.lightningkite.kiteui.map.style

import com.lightningkite.kiteui.map.style.LayerValue.Literal
import com.lightningkite.kiteui.models.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.spatialk.geojson.GeoJsonObject
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * The value of a layer property: either a [Literal] value, or an [Expression] that MapLibre
 * evaluates.
 *
 * See https://maplibre.org/maplibre-style-spec/expressions/
 */
sealed interface LayerValue<out T, P> {
    /**
     * A literal [value]. Create with `literal`, e.g. `Color.red.literal`. Only those extensions can
     * create one, so [value] is always a type that [toJson] can encode.
     */
    class Literal<out T, P> internal constructor(val value: T) : LayerValue<T, P>
}

class ExpressionBuilder<P> {
    operator fun <T> LayerValue<List<T>, *>.get(index: LayerValue<Number, *>) =
        Expression<T, P>("at", index, this)

    fun <T> LayerValue<List<T>, *>.contains(x: LayerValue<T, *>) = Expression<Boolean, P>("in", x, this)
    @JvmName("stringContains")
    fun LayerValue<String, *>.contains(x: LayerValue<String, *>) = Expression<Boolean, P>("in", x, this)
    fun <T> LayerValue<List<T>, *>.indexOf(x: LayerValue<T, *>) = Expression<Number, P>("index-of", x, this)
    fun <T> LayerValue<List<T>, *>.slice(inclusiveStart: LayerValue<Number, *>, exclusiveEnd: LayerValue<Number, *>? = null) =
        Expression<List<T>, P>("slice", this, inclusiveStart, exclusiveEnd ?: Literal<Nothing?, P>(null))

    val <T> KProperty1<P, T>.get get() = Expression<T, P>("get", Literal<String, P>(name))

    fun <T> case(cases: List<Pair<LayerValue<Boolean, *>, LayerValue<T, *>>>) =
        Expression<T, P>("case", *cases.flatMap { listOf(it.first, it.second) }.toTypedArray())

    infix fun <T> LayerValue<T, P>.eq(other: LayerValue<T, P>) = Expression<Boolean, P>("==", this, other)
    infix fun <T> LayerValue<T, P>.neq(other: LayerValue<T, P>) = Expression<Boolean, P>("!=", this, other)
    infix fun LayerValue<Number, P>.gt(other: LayerValue<Number, P>) = Expression<Boolean, P>(">", this, other)
    infix fun LayerValue<Number, P>.lt(other: LayerValue<Number, P>) = Expression<Boolean, P>("<", this, other)
    infix fun LayerValue<Number, P>.gte(other: LayerValue<Number, P>) = Expression<Boolean, P>(">=", this, other)
    infix fun LayerValue<Number, P>.lte(other: LayerValue<Number, P>) = Expression<Boolean, P>("<=", this, other)
    fun all(vararg of: LayerValue<Boolean, P>) = Expression<Boolean, P>("all", *of)
    fun any(vararg of: LayerValue<Boolean, P>) = Expression<Boolean, P>("any", *of)
    operator fun LayerValue<Boolean, P>.not() = Expression<Boolean, P>("!", this)
    val LayerValue<GeoJsonObject, P>.isWithin get() = Expression<Boolean, P>("within", this)

    operator fun LayerValue<Number, *>.plus(other: LayerValue<Number, *>) = Expression<Number, P>("+", this, other)
    fun sum(vararg layerValues: LayerValue<Number, *>) = Expression<Number, P>("+", *layerValues)
    operator fun minus(other: LayerValue<Number, *>) = Expression<Number, P>("-", other)
    operator fun LayerValue<Number, *>.minus(other: LayerValue<Number, *>) = Expression<Number, P>("-", this, other)
    operator fun LayerValue<Number, *>.times(other: LayerValue<Number, *>) = Expression<Number, P>("*", this, other)
    operator fun LayerValue<Number, *>.div(other: LayerValue<Number, *>) = Expression<Number, P>("/", this, other)
    operator fun LayerValue<Number, *>.rem(other: LayerValue<Number, *>) = Expression<Number, P>("%", this, other)
    fun LayerValue<Number, *>.pow(other: LayerValue<Number, *>) = Expression<Number, P>("^", this, other)
    fun sqrt(x: LayerValue<Number, *>) = Expression<Number, P>("sqrt", x)
    fun ln(x: LayerValue<Number, *>) = Expression<Number, P>("ln", x)
    fun log2(x: LayerValue<Number, *>) = Expression<Number, P>("log2", x)
    fun log10(x: LayerValue<Number, *>) = Expression<Number, P>("log10", x)
    fun sin(x: LayerValue<Number, *>) = Expression<Number, P>("sin", x)
    fun cos(x: LayerValue<Number, *>) = Expression<Number, P>("cos", x)
    fun tan(x: LayerValue<Number, *>) = Expression<Number, P>("tan", x)
}

fun <T, P> expression(builder: ExpressionBuilder<P>.() -> Expression<T, P>) = ExpressionBuilder<P>().builder()

/** A MapLibre expression that evaluates to a [T], as written in the style spec ([json]). */
class Expression<out T, P> internal constructor(op: String, vararg elements: LayerValue<*, *>) : LayerValue<T, P> {
    val json = JsonArray(listOf(JsonPrimitive(op)) + elements.map { it.toJson() })

    override fun equals(other: Any?) = other is Expression<*, *> && json == other.json
    override fun hashCode() = json.hashCode()
    override fun toString() = json.toString()
}

/** The value as written in the style spec. */
internal fun LayerValue<*, *>.toJson(): JsonElement = when (this) {
    is Expression -> json
    is Literal -> when (val value = value) {
        is Point -> literalArray(listOf(value.x, value.y).map(::JsonPrimitive))
        is Padding -> literalArray(listOf(value.top, value.right, value.bottom, value.left).map(::JsonPrimitive))
        is List<*> -> literalArray(value.flatMap { item ->
            // Anchor offsets are flattened into `anchor, [x, y]` pairs.
            if (item is SymbolLayer.AnchorOffset) {
                listOf(
                    item.anchor.json,
                    JsonArray(listOf(JsonPrimitive(item.offset.x), JsonPrimitive(item.offset.y)))
                )
            } else {
                listOf(scalarJson(item))
            }
        })

        else -> scalarJson(value)
    }
}

private fun literalArray(items: List<JsonElement>) = JsonArray(listOf(JsonPrimitive("literal"), JsonArray(items)))

private fun scalarJson(value: Any?): JsonElement = when (value) {
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Color -> JsonPrimitive(value.toWeb())
    is StyleEnum -> value.json
    else -> error("Unsupported literal value $value")
}

// Literal values


