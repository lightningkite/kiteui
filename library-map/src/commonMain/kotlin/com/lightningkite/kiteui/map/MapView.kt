package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ImageVector
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.lensing.lens
import com.lightningkite.services.data.GeoCoordinate
import com.lightningkite.services.database.HasId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.GeoJson
import org.maplibre.spatialk.geojson.Geometry
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.uuid.Uuid

/** A MapLibre wrapper. Supports Android, iOS, and Web. */
expect class MapView(
    context: ElementContext, disableAttribution: I_SWEAR_ON_MY_JOB?, interactive: Boolean
) : NativeElement, BaseProperties {
    internal val preInit: PreInit

    override var style: Style
    internal val images: MutableMap<Uuid, ImageSource>
    internal val sources: MutableMap<Uuid, Source<*, *>>
    internal val layers: MutableMap<Uuid, Layer<*>?>

    val camera: Camera
}

inline fun ElementWriter.mapView(
    disableAttribution: I_SWEAR_ON_MY_JOB? = null,
    interactive: Boolean = true,
    setup: MapView.() -> Unit,
) = write(MapView(context, disableAttribution, interactive), setup)

expect fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit)


inline fun <G : Geometry?, reified P : HasId<*>> MapView.addSource(
    feature: Reactive<Feature<G, P>>,
    noinline setup: Source<G, P>.() -> Unit = {},
) = addSource(feature, serializer<P>(), setup)

fun <G : Geometry?, P : HasId<*>> MapView.addSource(
    feature: Reactive<Feature<G, P>>,
    propertiesSerializer: KSerializer<P>,
    setup: Source<G, P>.() -> Unit = {},
) = Source(this, feature.lens { listOf(it) }, propertiesSerializer).apply(setup)

@JvmName("source2")
inline fun <G : Geometry?, reified P : HasId<*>> MapView.addSource(
    features: Reactive<Collection<Feature<G, P>>>,
    noinline setup: Source<G, P>.() -> Unit = {},
) = addSource(features, serializer<P>(), setup)

@JvmName("source2")
fun <G : Geometry?, P : HasId<*>> MapView.addSource(
    features: Reactive<Collection<Feature<G, P>>>,
    propertiesSerializer: KSerializer<P>,
    setup: Source<G, P>.() -> Unit = {},
) = Source(this, features, propertiesSerializer).apply(setup)

@JvmName("sourceNoProperties")
fun <G : Geometry?> MapView.addSource(
    feature: Reactive<Feature<G, Nothing?>>,
    setup: Source<G, Nothing?>.() -> Unit = {},
) = Source(this, feature.lens { listOf(it) }, serializer<Nothing?>()).apply(setup)

@JvmName("sourceNoProperties2")
fun <G : Geometry?> MapView.addSource(
    features: Reactive<Collection<Feature<G, Nothing?>>>,
    setup: Source<G, Nothing?>.() -> Unit = {},
) = Source(this, features, serializer<Nothing?>()).apply(setup)


class ResolvedImage internal constructor(val mapView: MapView) : Layer.Value<ImageSource> {
    val id = Uuid.random()
    override val json get() = expression<ImageSource>("image", id.toString().literal).json
}

fun MapView.addImage(source: ImageSource) = ResolvedImage(this).also { images[it.id] = source }
fun MapView.addImage(source: Reactive<ImageSource>) = ResolvedImage(this).also { reactive { images[it.id] = source() } }


/**
 * Yes, this is meant to be attention grabbing.
 *
 * You really need to handle attribution correctly, as it's potentially a legal issue. Check
 * all the third-party map APIs you are using and what kind of attribution they require, if any.
 * MapLibre itself does not require attribution, as it's licensed under 3-Clause BSD.
 *
 * The passed value isn't actually used for anything behind the scenes. It's just a form of in-code
 * docs (and a way to make it slightly harder to forget to handle it).
 *
 * As an example, basemaps obtained from `Map.Style.Url()` commonly require attribution.
 */
enum class I_SWEAR_ON_MY_JOB {
    /**
     * Choose this if you have checked and verified that none of the third party APIs that you
     * are using require attribution.
     */
    I_DO_NOT_NEED_ATTRIBUTION,

    /** Choose this if you are handling attribution manually elsewhere in the app. */
    I_AM_HANDLING_ATTRIBUTION,
}


/**
 * The MapLibre map is not initialized immediately on android and web, so we need to keep track
 * of state changes that happen before initialization (e.g., in the KiteUI initializer, which
 * runs first) and forward those to the initializer.
 */
internal data class PreInit(
    override var style: Style = Style.Demo,

    // Camera
    override var center: GeoCoordinate = GeoCoordinate(0.0, 0.0),
    override var zoom: Double = 0.0,
    override var minZoom: Double? = null,
    override var maxZoom: Double? = null,
    override var bearing: Double = 0.0,
    override var padding: Camera.Padding = Camera.Padding(),
    override var pitch: Double = 0.0,
    override var minPitch: Double? = null,
    override var maxPitch: Double? = null,
) : BaseProperties, CameraProperties

internal class StyleDataMap<V>(
    private val onPut: (key: Uuid, new: V) -> Unit,
    private val onRemove: (key: Uuid) -> Unit,
) : AbstractMutableMap<Uuid, V>() {
    private val backing = LinkedHashMap<Uuid, V>()

    override val size: Int get() = backing.size
    override fun get(key: Uuid): V? = backing[key]
    override fun containsKey(key: Uuid): Boolean = backing.containsKey(key)
    override fun put(key: Uuid, value: V): V? = backing.put(key, value).also { onPut(key, value) }
    override fun remove(key: Uuid): V? = backing.remove(key)?.also { onRemove(key) }

    override val entries: MutableSet<MutableMap.MutableEntry<Uuid, V>> =
        object : AbstractMutableSet<MutableMap.MutableEntry<Uuid, V>>() {
            override val size: Int get() = backing.size
            override fun add(element: MutableMap.MutableEntry<Uuid, V>): Boolean =
                put(element.key, element.value) != element.value

            override fun iterator(): MutableIterator<MutableMap.MutableEntry<Uuid, V>> =
                object : MutableIterator<MutableMap.MutableEntry<Uuid, V>> {
                    private val inner = backing.entries.iterator()
                    private var last: MutableMap.MutableEntry<Uuid, V>? = null

                    override fun hasNext() = inner.hasNext()

                    override fun next(): MutableMap.MutableEntry<Uuid, V> {
                        val entry = inner.next().also { last = it }
                        return object : MutableMap.MutableEntry<Uuid, V> {
                            override val key get() = entry.key
                            override val value get() = entry.value
                            override fun setValue(newValue: V) = put(entry.key, newValue)!!
                        }
                    }

                    override fun remove() {
                        val removed = checkNotNull(last) { "next() has not been called" }
                        val (key, value) = removed.key to removed.value
                        inner.remove()
                        last = null
                        onRemove(key)
                    }
                }
        }
}

internal class LateInit<T, M>(
    val mapView: MapView,
    val getMapState: MapView.() -> ReactiveState<M>,
    val preInitProp: KMutableProperty1<PreInit, T>,
    val getter: M.() -> T,
    val setter: M.(T) -> Unit,
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        mapView.getMapState().getOrNull()?.run { getter() } ?: preInitProp.get(mapView.preInit)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        preInitProp.set(mapView.preInit, value)
        mapView.getMapState().getOrNull()?.setter(value)
    }
}

sealed interface Style {
    data class Url(val url: String) : Style
    data class Json(val json: String) : Style

    companion object {
        val Demo = Url("https://demotiles.maplibre.org/style.json")
        val Debug = Url("https://demotiles.maplibre.org/debug-tiles/style.json")
        val GlobeDemo = Url("https://demotiles.maplibre.org/globe.json")
    }
}

interface BaseProperties {
    var style: Style
}

interface CameraProperties {
    var center: GeoCoordinate
    var zoom: Double
    var minZoom: Double?
    var maxZoom: Double?
    var bearing: Double
    var padding: Camera.Padding
    var pitch: Double
    var minPitch: Double?
    var maxPitch: Double?
}

interface Camera : CameraProperties {
    fun ease(animateTo: AnimateTo)
    fun fly(animateTo: AnimateTo)
    fun stopAnimation()

    data class Padding(
        val top: Double = 0.0,
        val bottom: Double = 0.0,
        val right: Double = 0.0,
        val left: Double = 0.0,
    )

    data class AnimateTo(
        val center: GeoCoordinate? = null,
        val zoom: Double? = null,
        val bearing: Double? = null,
        val padding: Padding? = null,
        val pitch: Double? = null,
        val duration: Duration,
    )
}

internal fun MapView.handleSourceLayerClicks(getClickedProperties: (layerIds: Set<Uuid>) -> List<JsonObject?>): Boolean {
    val layersBySource = layers.values.filterNotNull().groupBy { it.source }.mapKeys { sources[it.key]!! }
    var handledAny = false

    layersBySource.forEach { (source, layers) ->
        @Suppress("UNCHECKED_CAST") source as Source<*, HasId<*>?>

        layers.forEach { layer ->
            if (layer.clickListeners.isEmpty()) return@forEach
            @Suppress("UNCHECKED_CAST") layer as Layer<HasId<*>?>
            val properties = getClickedProperties(setOf(layer.id)).map(source::decodeProperties).distinctBy { it?._id }
            if (properties.isEmpty()) return@forEach
            layer.clickListeners.forEach { it(properties); handledAny = true }
        }

        if (source.clickListeners.isEmpty()) return@forEach
        val properties =
            getClickedProperties(layers.map { it.id }.toSet()).map(source::decodeProperties).distinctBy { it?._id }
        if (properties.isEmpty()) return@forEach
        source.clickListeners.forEach { it(properties); handledAny = true }
    }

    return handledAny
}

private fun <P : HasId<*>?> Source<*, P>.decodeProperties(properties: JsonObject?): P {
    val descriptor = propertiesSerializer.descriptor
    // MapLibre reports a feature with null properties as having empty properties.
    val json = if (properties == null || properties.isEmpty() && descriptor.isNullable) JsonNull
    else properties.restoreNestedValues(descriptor)
    return GeoJson.jsonFormat.decodeFromJsonElement(propertiesSerializer, json)
}

/**
 * MapLibre (at least on the web) flattens nested objects and arrays in feature properties into
 * JSON strings, so this parses them back wherever [descriptor] expects a structure.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun JsonObject.restoreNestedValues(descriptor: SerialDescriptor): JsonObject {
    if (descriptor.kind != StructureKind.CLASS) return this
    return JsonObject(mapValues { (key, value) ->
        val index = descriptor.getElementIndex(key)
        if (index == CompositeDecoder.UNKNOWN_NAME || value !is JsonPrimitive || !value.isString) return@mapValues value
        when (descriptor.getElementDescriptor(index).kind) {
            is StructureKind, is PolymorphicKind ->
                runCatching { Json.parseToJsonElement(value.content) }.getOrDefault(value)
            else -> value
        }
    })
}

fun getDefaultPin(
    size: Size = Size(20.0 * 1.2, 32.0 * 1.2),
    color: Color = Color.red,
    strokeColor: Color = color.toBlack(0.25f),
    dotColor: Color = Color.white,
) = ImageVector(
    width = size.width.dp,
    height = size.height.dp,
    viewBoxWidth = 20,
    viewBoxHeight = 32,
    paths = listOf(
        ImageVector.Path(
            fillColor = color,
            strokeColor = strokeColor,
            strokeWidth = 1.0,
            strokeCap = Icon.StrokeLineCap.Round,
            path = "M19.5,10.4 c0,6.3 -9.5,17.1 -9.5,17.1 S0.5,16.6 0.5,10.4 c0,-5.5 4.3,-9.9 9.5,-9.9 S19.5,4.9 19.5,10.4 Z",
        ),
        ImageVector.Path(
            fillColor = dotColor,
            strokeColor = strokeColor,
            strokeWidth = 1.0,
            strokeCap = Icon.StrokeLineCap.Round,
            path = "M6.2,10 A3.8,3.8 0 1,0 13.8,10 A3.8,3.8 0 1,0 6.2,10 Z",
        ),
    ),
)