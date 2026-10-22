package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.Layer.Literal
import com.lightningkite.kiteui.map.Layer.Value
import com.lightningkite.kiteui.models.Color
import com.lightningkite.services.data.GeoCoordinate
import com.lightningkite.services.database.HasId
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
    internal val properties = ArrayList<Property<*>>()
    internal val clickListeners = ArrayList<(properties: List<P>) -> Unit>()

    var minZoom: Double? = null
        set(value) {
            field = value
            if (isLive) setLiveMinZoom(value)
        }

    var maxZoom: Double? = null
        set(value) {
            field = value
            if (isLive) setLiveMaxZoom(value)
        }

    internal val isLive: Boolean get() = mapView.layers[id] === this && isNativeLayerAdded

    internal inner class Property<T>(
        val group: PropertyGroup,
        val name: String,
        val cocoaName: String,
        val native: Boolean,
    ) : ReadWriteProperty<Layer<P>, Value<T>?> {
        init {
            properties += this
        }

        internal var value: Value<T>? = null

        override fun getValue(thisRef: Layer<P>, property: KProperty<*>): Value<T>? = value

        override fun setValue(thisRef: Layer<P>, property: KProperty<*>, value: Value<T>?) {
            this.value = value
            when {
                !native -> if (mapView.layers[id] === thisRef) mapView.layers[id] = thisRef
                isLive -> writeLiveProperty(this, value?.json)
            }
        }
    }

    internal enum class PropertyGroup { Paint, Layout, Filter }

    internal fun filter() = Property<Boolean>(PropertyGroup.Filter, "filter", "predicate", native = true)

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


    sealed interface Value<out T> {
        val json: JsonElement
    }

    internal class Literal<T>(override val json: JsonElement) : Value<T>

    internal interface StyleEnum<T> : Value<T> {
        override val json get() = JsonPrimitive(value)
        val value: String
    }

    data class Point(val x: Double, val y: Double) : Value<Point> {
        override val json get() = JsonArray(listOf(JsonPrimitive(x), JsonPrimitive(y)))
    }

    val GeoCoordinate.point get() = Point(this.longitude, this.latitude)

    data class Padding(val top: Double, val right: Double, val bottom: Double, val left: Double) : Value<Padding> {
        constructor(all: Double) : this(all, all, all, all)
        constructor(vertical: Double, horizontal: Double) : this(vertical, horizontal, vertical, horizontal)

        override val json get() = JsonArray(listOf(top, right, bottom, left).map(::JsonPrimitive))
    }

    data class AnchorOffsets(val value: List<Pair<SymbolLayer.Anchor, Point>>) : Value<AnchorOffsets> {
        override val json get() = JsonArray(value.flatMap { listOf(it.first, it.second) }.map { it.json })
    }

    val <T> List<Value<T>>.literal get() = expression<List<T>>("semiliteral", Literal<Any>(JsonArray(map { it.json })))

    operator fun <T> Value<List<T>>.get(index: Value<Number>) = expression<T>("at", index, this)

    @JvmName("stringContains")
    fun Value<String>.contains(x: Value<String>) = expression<Boolean>("in", x, this)
    fun <T> Value<List<T>>.contains(x: Value<T>) = expression<Boolean>("in", x, this)
    fun <T> Value<List<T>>.indexOf(x: Value<T>) = expression<Number>("index-of", x, this)
    fun <T> Value<List<T>>.slice(
        inclusiveStart: Value<Number>, exclusiveEnd: Value<Number>? = null
    ) = expression<List<T>>("slice", this, inclusiveStart, exclusiveEnd.flatten)

    val <T> KProperty1<P, T>.value get() = expression<T>("get", Literal<T>(JsonPrimitive(name)))
    fun <T> Value<List<T>>.size() = expression<Number>("length", this)

    fun <T> case(cases: List<Pair<Value<Boolean>, Value<T>>>, fallback: Value<T>) =
        expression<T>("case", *cases.flatMap { listOf(it.first, it.second) }.toTypedArray(), fallback)

    fun <T> List<Value<T?>>.firstNotNullOr(default: Value<T>) = expression<T>("coalesce", *this.toTypedArray(), default)
    infix fun <T> Value<T>.eq(other: Value<T>) = expression<Boolean>("==", this, other)
    infix fun <T> Value<T>.neq(other: Value<T>) = expression<Boolean>("!=", this, other)
    infix fun Value<Number>.gt(other: Value<Number>) = expression<Boolean>(">", this, other)
    infix fun Value<Number>.lt(other: Value<Number>) = expression<Boolean>("<", this, other)
    infix fun Value<Number>.gte(other: Value<Number>) = expression<Boolean>(">=", this, other)
    infix fun Value<Number>.lte(other: Value<Number>) = expression<Boolean>("<=", this, other)

    @JvmName("stringGt")
    infix fun Value<String>.gt(other: Value<String>) = expression<Boolean>(">", this, other)

    @JvmName("stringLt")
    infix fun Value<String>.lt(other: Value<String>) = expression<Boolean>("<", this, other)

    @JvmName("stringGte")
    infix fun Value<String>.gte(other: Value<String>) = expression<Boolean>(">=", this, other)

    @JvmName("stringLte")
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
    operator fun Value<Number>.unaryMinus() = expression<Number>("-", this)
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

    fun Value<String>.uppercase() = expression<String>("upcase", this)
    fun Value<String>.lowercase() = expression<String>("downcase", this)

    @JvmName("stringPlus")
    operator fun Value<String>.plus(other: Value<String>) = expression<String>("concat", this, other)
    fun Value<String>.split(separator: Value<String>) = expression<List<String>>("split", this, separator)
    fun Value<List<String>>.joinToString(separator: Value<String>) = expression<String>("join", this, separator)
}


val <T> Value<T>?.flatten get(): Value<T?> = this ?: Literal(JsonPrimitive(null))
val Number.literal get(): Value<Number> = Literal(JsonPrimitive(this))
val Boolean.literal get(): Value<Boolean> = Literal(JsonPrimitive(this))
val String.literal get(): Value<String> = Literal(JsonPrimitive(this))
val Color.literal get(): Value<Color> = Literal(JsonPrimitive(toWeb()))

internal fun <T> expression(op: String, vararg elements: Value<*>): Value<T> =
    Literal(JsonArray(listOf(JsonPrimitive(op)) + elements.map { it.json }))

fun <P : HasId<*>> Layer<P>.onClick(callback: (properties: List<P>) -> Unit) {
    clickListeners += callback
}

fun Layer<Nothing?>.onClick(callback: () -> Unit) {
    clickListeners += { callback() }
}

internal expect val Layer<*>.isNativeLayerAdded: Boolean
internal expect fun Layer<*>.writeLiveProperty(prop: Layer<*>.Property<*>, value: JsonElement?)
internal expect fun Layer<*>.setLiveMinZoom(value: Double?)
internal expect fun Layer<*>.setLiveMaxZoom(value: Double?)