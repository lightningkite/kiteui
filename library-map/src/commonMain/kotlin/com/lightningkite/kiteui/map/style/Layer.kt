package com.lightningkite.kiteui.map.style

import com.lightningkite.kiteui.map.MapView
import com.lightningkite.kiteui.map.style.LayerValue.Literal
import com.lightningkite.kiteui.models.Color
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.uuid.Uuid

/**
 * A layer of a [MapView], drawing one of its sources.
 *
 * Layers know which map they belong to, so their properties can be set at any time. Properties are
 * stored (and read back as set), and written through to the platform's layer while it is on the
 * map. Before then (e.g. while the style loads), they are applied once it is added.
 */
sealed class Layer<P>(
    internal val mapView: MapView,
    /** This layer's key in `MapView.layers`. */
    val id: Uuid,
    /** The id of the source this layer draws, as a key of `MapView.sources`. */
    val source: Uuid,
    /** The style spec's name for this type of layer, like `circle`. */
    private val type: String,
) {
    private val properties = ArrayList<Property<*>>()

    /** The minimum zoom level for the layer. At lower zoom levels, the layer is hidden. */
    var minZoom: Double? = null
        set(value) {
            field = value
            if (isLive) setNativeMinZoom(value)
        }

    /** The maximum zoom level for the layer. At and above this zoom level, the layer is hidden. */
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
    ) : ReadWriteProperty<Layer<P>, LayerValue<T, P>?> {
        internal val name get() = key.name
        internal val group get() = key.group
        internal var value: LayerValue<T, P>? = null

        override fun getValue(thisRef: Layer<P>, property: KProperty<*>): LayerValue<T, P>? = value

        override fun setValue(thisRef: Layer<P>, property: KProperty<*>, value: LayerValue<T, P>?) {
            this.value = value
            when {
                !native -> if (mapView.layers[id] === thisRef) mapView.layers[id] = thisRef
                isLive -> writeNativeProperty(key, value?.toJson())
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
        fun group(group: PropertyGroup) =
            properties.filter { it.group == group }.mapNotNull { property -> property.value?.let { property.name to it.toJson() } }

        put("type", type)
        put("source", source.toString())
        group(PropertyGroup.Filter).singleOrNull()?.let { put("filter", it.second) }
        minZoom?.let { put("minzoom", it) }
        maxZoom?.let { put("maxzoom", it) }
        put("paint", JsonObject(group(PropertyGroup.Paint).toMap()))
        put("layout", JsonObject(group(PropertyGroup.Layout).toMap()))
    }


    val <T> LayerValue<T, *>?.flatten get() = this ?: Literal<T?, P>(null)
    val Number.literal get() = Literal<Number, P>(this)
    val Boolean.literal get() = Literal<Boolean, P>(this)
    val String.literal get() = Literal<String, P>(this)
    val Color.literal get() = Literal<Color, P>(this)
    val <E : StyleEnum> E.literal get() = Literal<E, P>(this)
    val Point.literal get() = Literal<Point, P>(this)
    val Padding.literal get() = Literal<Padding, P>(this)

    @get:JvmName("numbersLiteral")
    val List<Number>.literal get() = Literal<List<Number>, P>(this)

    @get:JvmName("stringsLiteral")
    val List<String>.literal get() = Literal<List<String>, P>(this)

    @get:JvmName("colorsLiteral")
    val List<Color>.literal get() = Literal<List<Color>, P>(this)

    @get:JvmName("enumsLiteral")
    val <E : StyleEnum> List<E>.literal get() = Literal<List<E>, P>(this)

    @get:JvmName("anchorOffsetsLiteral")
    val List<SymbolLayer.AnchorOffset>.literal get() = Literal<List<SymbolLayer.AnchorOffset>, P>(this)
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

/** Implemented by the generated enums of style spec values. */
interface StyleEnum {
    /** The value as written in the style spec. */
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
