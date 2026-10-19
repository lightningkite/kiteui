package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.models.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.GeoJsonObject
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.uuid.Uuid

sealed class Layer<P>(
    internal val mapView: MapView,
    val id: Uuid,
    val source: Uuid,
    private val type: String,
) {
    private val properties = ArrayList<Property<*>>()

    var minZoom: Double? = null
        set(value) {
            field = value
            if (isLive) setNativeMinZoom(value)
        }

    var maxZoom: Double? = null
        set(value) {
            field = value
            if (isLive) setNativeMaxZoom(value)
        }

    /**
     * Whether this layer is on the map, so property changes should go through the platform's layer.
     * A layer that has been replaced in `MapView.layers` isn't live, even though its id is.
     */
    internal val isLive: Boolean get() = mapView.layers[id] === this && isNativeLayerAdded

    /**
     * A style spec property. While the layer is live, setting it writes it to the platform's layer
     * if it is [native] (supported on every platform), or else re-adds the layer.
     */
    internal inner class Property<T>(
        private val key: PropertyKey,
        private val native: Boolean,
    ) : ReadWriteProperty<Layer<P>, Value<T>?> {
        internal val name get() = key.name
        internal val group get() = key.group
        internal var value: Value<T>? = null

        override fun getValue(thisRef: Layer<P>, property: KProperty<*>): Value<T>? = value

        override fun setValue(thisRef: Layer<P>, property: KProperty<*>, value: Value<T>?) {
            this.value = value
            when {
                !native -> if (mapView.layers[id] === thisRef) mapView.layers[id] = thisRef
                isLive -> writeNativeProperty(key, value?.json)
            }
        }
    }

    private fun <T> property(key: PropertyKey, native: Boolean) = Property<T>(key, native).also { properties += it }

    internal fun <T> paint(name: String, cocoaName: String, native: Boolean = true) =
        property<T>(PropertyKey(PropertyGroup.Paint, name, cocoaName), native)

    internal fun <T> layout(name: String, cocoaName: String, native: Boolean = true) =
        property<T>(PropertyKey(PropertyGroup.Layout, name, cocoaName), native)

    internal fun filter() = property<Boolean>(PropertyKey(PropertyGroup.Filter, "filter", "predicate"), native = true)

    /** The layer as style spec JSON. */
    internal fun toJson(): JsonObject = buildJsonObject {
        fun group(group: PropertyGroup) = properties.filter { it.group == group }
            .mapNotNull { property -> property.value?.let { property.name to it.json } }

        put("type", type)
        put("source", source.toString())
        group(PropertyGroup.Filter).singleOrNull()?.let { put("filter", it.second) }
        minZoom?.let { put("minzoom", it) }
        maxZoom?.let { put("maxzoom", it) }
        put("paint", JsonObject(group(PropertyGroup.Paint).toMap()))
        put("layout", JsonObject(group(PropertyGroup.Layout).toMap()))
    }

//    @get:JvmName("numbersLiteral")
//    val List<Number>.literal get() = Literal<List<Number>, P>(this)

//    @get:JvmName("stringsLiteral")
//    val List<String>.literal get() = Literal<List<String>, P>(this)

//    @get:JvmName("colorsLiteral")
//    val List<Color>.literal get() = Literal<List<Color>, P>(this)

//    @get:JvmName("enumsLiteral")
//    val <E : StyleEnum> List<E>.literal get() = Literal<List<E>, P>(this)

//    @get:JvmName("anchorOffsetsLiteral")
//    val List<SymbolLayer.AnchorOffset>.literal get() = Literal<List<SymbolLayer.AnchorOffset>, P>(this)


    class Value<out T> internal constructor(val json: JsonElement) {
//        fun toJson(): JsonElement = when (this) {
//            is Expression -> json
//            is Literal -> when (val value = value) {
//                is Point -> literalArray(listOf(value.x, value.y).map(::JsonPrimitive))
//                is Padding -> literalArray(
//                    listOf(
//                        value.top,
//                        value.right,
//                        value.bottom,
//                        value.left
//                    ).map(::JsonPrimitive)
//                )
//
//                is List<*> -> literalArray(value.flatMap { item ->
//                     Anchor offsets are flattened into `anchor, [x, y]` pairs.
//                    if (item is SymbolLayer.AnchorOffset) {
//                        listOf(
//                            item.anchor.json,
//                            JsonArray(listOf(JsonPrimitive(item.offset.x), JsonPrimitive(item.offset.y)))
//                        )
//                    } else {
//                        listOf(scalarJson(item))
//                    }
//                })
//
//                else -> scalarJson(value)
//            }
//        }
    }

//    class Literal<out T> internal constructor(val value: T) : Value<T>

    val <T> Value<T>?.flatten get() = this ?: Value<T?>(JsonPrimitive(null))
    val Number.literal get() = Value<Number>(JsonPrimitive(this))
    val Boolean.literal get() = Value<Boolean>(JsonPrimitive(this))
    val String.literal get() = Value<String>(JsonPrimitive(this))
//    val Color.literal get() = Value<Color>(JsonPrimitive(this))
    val <E : StyleEnum> E.literal get() = Value<E>(JsonPrimitive(value))
//    val Point.literal get() = Value<Point>(JsonPrimitive(this))
//    val Padding.literal get() = Value<Padding>(JsonPrimitive(this))

//    class Expression<out T> internal constructor(op: String, vararg elements: Value<*>) : Value<T> {
//        val json = JsonArray(listOf(JsonPrimitive(op)) + elements.map { it.toJson() })

//        override fun equals(other: Any?) = other is Expression<*> && json == other.json
//        override fun hashCode() = json.hashCode()
//        override fun toString() = json.toString()
//    }

    internal fun <T> expression(op: String, vararg elements: Value<*>) =
        Value<T>(JsonArray(listOf(JsonPrimitive(op)) + elements.map { it.json }))

    operator fun <T> Value<List<T>>.get(index: Value<Number>) = expression<T>("at", index, this)

    @JvmName("stringContains")
    fun Value<String>.contains(x: Value<String>) = expression<Boolean>("in", x, this)
    fun <T> Value<List<T>>.contains(x: Value<T>) = expression<Boolean>("in", x, this)
    fun <T> Value<List<T>>.indexOf(x: Value<T>) = expression<Number>("index-of", x, this)
    fun <T> Value<List<T>>.slice(
        inclusiveStart: Value<Number>, exclusiveEnd: Value<Number>? = null
    ) = expression<List<T>>("slice", this, inclusiveStart, exclusiveEnd.flatten)

    val <T> KProperty1<P, T>.value get() = expression<T>("get", Value<String>(JsonPrimitive(name)))
    fun <T> Value<List<T>>.size() = expression<Number>("length", this)


    fun <T> case(cases: List<Pair<Value<Boolean>, Value<T>>>) =
        expression<T>("case", *cases.flatMap { listOf(it.first, it.second) }.toTypedArray())

    fun <T> List<Value<T?>>.firstNotNullOr(default: Value<T>) = expression<T>("coalesce", *this.toTypedArray(), default)
    infix fun <T> Value<T>.eq(other: Value<T>) = expression<Boolean>("==", this, other)
    infix fun <T> Value<T>.neq(other: Value<T>) = expression<Boolean>("!=", this, other)
    infix fun Value<Number>.gt(other: Value<Number>) = expression<Boolean>(">", this, other)
    infix fun Value<Number>.lt(other: Value<Number>) = expression<Boolean>("<", this, other)
    infix fun Value<Number>.gte(other: Value<Number>) = expression<Boolean>(">=", this, other)
    infix fun Value<Number>.lte(other: Value<Number>) = expression<Boolean>("<=", this, other)
    infix fun Value<String>.gt(other: Value<String>) = expression<Boolean>(">", this, other)
    infix fun Value<String>.lt(other: Value<String>) = expression<Boolean>("<", this, other)
    infix fun Value<String>.gte(other: Value<String>) = expression<Boolean>(">=", this, other)
    infix fun Value<String>.lte(other: Value<String>) = expression<Boolean>("<=", this, other)
    fun all(vararg of: Value<Boolean>) = expression<Boolean>("all", *of)
    fun any(vararg of: Value<Boolean>) = expression<Boolean>("any", *of)
    operator fun Value<Boolean>.not() = expression<Boolean>("!", this)
    val Value<GeoJsonObject>.isWithin get() = expression<Boolean>("within", this)


    fun <T> Value<Number>.step(
        initial: Value<T>, steps: List<Pair<Value<Number>, Value<T>>>
    ) = expression<T>("step", this, initial, *steps.flatMap { listOf(it.first, it.second) }.toTypedArray())


    operator fun Value<Number>.plus(other: Value<Number>) = expression<Number>("+", this, other)
    fun sum(vararg values: Value<Number>) = expression<Number>("+", *values)
    operator fun minus(other: Value<Number>) = expression<Number>("-", other)
    operator fun Value<Number>.minus(other: Value<Number>) = expression<Number>("-", this, other)
    operator fun Value<Number>.times(other: Value<Number>) = expression<Number>("*", this, other)
    operator fun Value<Number>.div(other: Value<Number>) = expression<Number>("/", this, other)
    operator fun Value<Number>.rem(other: Value<Number>) = expression<Number>("%", this, other)
    fun Value<Number>.pow(other: Value<Number>) = expression<Number>("^", this, other)
    fun sqrt(x: Value<Number>) = expression<Number>("sqrt", x)
    fun ln(x: Value<Number>) = expression<Number>("ln", x)
    fun log2(x: Value<Number>) = expression<Number>("log2", x)
    fun log10(x: Value<Number>) = expression<Number>("log10", x)
    fun sin(x: Value<Number>) = expression<Number>("sin", x)
    fun cos(x: Value<Number>) = expression<Number>("cos", x)
    fun tan(x: Value<Number>) = expression<Number>("tan", x)
    fun asin(x: Value<Number>) = expression<Number>("asin", x)
    fun acos(x: Value<Number>) = expression<Number>("acos", x)
    fun atan(x: Value<Number>) = expression<Number>("atan", x)
    fun min(vararg of: Value<Number>) = expression<Number>("min", *of)
    fun max(vararg of: Value<Number>) = expression<Number>("max", *of)
    fun round(x: Value<Number>) = expression<Number>("round", x)
    fun abs(x: Value<Number>) = expression<Number>("abs", x)
    fun ceil(x: Value<Number>) = expression<Number>("ceil", x)
    fun floor(x: Value<Number>) = expression<Number>("floor", x)


    val zoom get() = expression<Number>("zoom")


    fun Value<String>.uppercase() = expression<Number>("upcase", this)
    fun Value<String>.lowercase() = expression<Number>("downcase", this)
    operator fun Value<String>.plus(other: Value<String>) = expression<String>("concat", this, other)
    fun Value<String>.split(separator: Value<String>) = expression<List<String>>("split", this, separator)
    fun Value<List<String>>.joinToString(separator: Value<String>) = expression<String>("join", this, separator)
}

private fun literalArray(items: List<JsonElement>) =
    JsonArray(listOf(JsonPrimitive("literal"), JsonArray(items)))

private fun scalarJson(value: Any?): JsonElement = when (value) {
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Color -> JsonPrimitive(value.toWeb())
    is StyleEnum -> value.json
    else -> error("Unsupported literal value $value")
}

fun Layer<*>.onClick(callback: () -> Unit) {

}

internal enum class PropertyGroup { Paint, Layout, Filter }

/**
 * Identifies a layer property on every platform: by [group] and style spec [name], or on iOS by
 * [cocoaName], the name of the `MLN*StyleLayer` property (which renames some).
 */
internal class PropertyKey(val group: PropertyGroup, val name: String, val cocoaName: String)

/** Whether the platform's map currently has this layer's id. */
internal expect val Layer<*>.isNativeLayerAdded: Boolean

/** Writes a property, as style spec JSON, to the platform's live layer. Only called while [Layer.isLive]. */
internal expect fun Layer<*>.writeNativeProperty(key: PropertyKey, value: JsonElement?)

/** Sets the platform's live layer's minimum zoom, or resets it if null. Only called while [Layer.isLive]. */
internal expect fun Layer<*>.setNativeMinZoom(value: Double?)

/** Sets the platform's live layer's maximum zoom, or resets it if null. Only called while [Layer.isLive]. */
internal expect fun Layer<*>.setNativeMaxZoom(value: Double?)

interface StyleEnum {
    val value: String
}

/** An `[x, y]` offset. Positive values are right and down. */
data class Point(val x: Double, val y: Double)

/** Padding in the style spec's `[top, right, bottom, left]` order. */
data class Padding(val top: Double, val right: Double, val bottom: Double, val left: Double) {
    constructor(all: Double) : this(all, all, all, all)
    constructor(vertical: Double, horizontal: Double) : this(vertical, horizontal, vertical, horizontal)
}

/**
 * Text that may contain sections with different fonts, scales, and colors. Create with [format], or
 * convert a string expression with [formatted].
 */
sealed interface Formatted

/** An image from the style's sprite or added to the map. Create with [image]. */
sealed interface ResolvedImage

internal val StyleEnum.json: JsonElement get() = JsonPrimitive(value)


