package com.lightningkite.kiteui.map

import cocoapods.MapLibre.*
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.direct.load
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import platform.CoreGraphics.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.*
import platform.QuartzCore.CAMediaTimingFunction
import platform.QuartzCore.kCAMediaTimingFunctionEaseInEaseOut
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName
import kotlin.uuid.Uuid

actual class MapView actual constructor(
    context: ElementContext,
    val disableAttribution: I_SWEAR_ON_MY_JOB?,
    val interactive: Boolean,
) : NativeElement(context), BaseProperties {

    internal actual val preInit = PreInit()

    internal val mlnMapView: MLNMapView = MLNMapView()
    override val native: UIView get() = mlnMapView

    private var isStyleLoaded = false
    internal val clickListeners = ArrayList<(where: GeoCoordinate) -> Unit>()

    // ---- Style ----

    actual override var style: Style = Style.Demo
        set(value) {
            field = value
            isStyleLoaded = false
            when (value) {
                is Style.Url -> mlnMapView.styleURL = NSURL.URLWithString(value.url)!!
                is Style.Json -> mlnMapView.styleJSON = value.json
            }
        }

    // ---- Sources ----

    actual val sources: MutableMap<Uuid, Source<*, *>> = StyleDataMap({ id, source ->
        if (!isStyleLoaded) return@StyleDataMap
        val style = mlnMapView.style ?: return@StyleDataMap
        val existing = style.sourceWithIdentifier(id.toString()) as? MLNShapeSource
        if (existing != null) {
            existing.shape = shapeFromGeoJson(source.data)
        } else {
            style.addSource(MLNShapeSource(id.toString(), shapeFromGeoJson(source.data), null))
        }
    }) { id ->
        if (!isStyleLoaded) return@StyleDataMap
        val style = mlnMapView.style ?: return@StyleDataMap
        style.sourceWithIdentifier(id.toString())?.let { style.removeSource(it) }
    }

    // ---- Layers ----

    actual val layers: MutableMap<Uuid, Layer<*>?> = StyleDataMap({ id, layer ->
        if (!isStyleLoaded) return@StyleDataMap
        val style = mlnMapView.style ?: return@StyleDataMap
        val beforeIndex = layers.keys.indexOf(id).let { if (0 <= it && it < layers.size - 1) it + 1 else null }
        val beforeLayer = beforeIndex?.let { style.layerWithIdentifier(layers.keys.toList()[it].toString()) }

        style.layerWithIdentifier(id.toString())?.let { style.removeLayer(it) }
        val nativeLayer = toMLNLayer(id.toString(), layer, style)
        if (beforeLayer == null) {
            style.addLayer(nativeLayer)
        } else {
            style.insertLayer(nativeLayer, belowLayer = beforeLayer)
        }
    }) { id ->
        if (!isStyleLoaded) return@StyleDataMap
        val style = mlnMapView.style ?: return@StyleDataMap
        style.layerWithIdentifier(id.toString())?.let { style.removeLayer(it) }
    }

    // ---- Images ----

    private val loadedImages = HashMap<Uuid, UIImage>()

    actual val images: MutableMap<Uuid, ImageSource> = StyleDataMap({ id, source ->
        launch {
            val image = source.load(null) ?: return@launch
            if (images[id] !== source) return@launch
            loadedImages[id] = image
            if (isStyleLoaded) mlnMapView.style?.setImage(image, forName = id.toString())
        }
    }) { id ->
        loadedImages.remove(id)
        if (isStyleLoaded) mlnMapView.style?.removeImageForName(id.toString())
    }

    // ---- Camera ----

    actual val camera = object : Camera {
        override var center: GeoCoordinate
            get() = mlnMapView.centerCoordinate.toGeoCoordinate()
            set(value) = mlnMapView.setCenterCoordinate(value.toCLCoordinate(), animated = false)

        override var zoom: Double
            get() = mlnMapView.zoomLevel
            set(value) = mlnMapView.setZoomLevel(value, animated = false)

        override var minZoom: Double?
            get() = mlnMapView.minimumZoomLevel
            set(value) { mlnMapView.minimumZoomLevel = value ?: 0.0 }

        override var maxZoom: Double?
            get() = mlnMapView.maximumZoomLevel
            set(value) { mlnMapView.maximumZoomLevel = value ?: 22.0 }

        override var bearing: Double
            get() = mlnMapView.direction
            set(value) = mlnMapView.setDirection(value, animated = false)

        override var padding: Camera.Padding
            get() = mlnMapView.contentInset.useContents {
                Camera.Padding(top = top, bottom = bottom, right = right, left = left)
            }
            set(value) { mlnMapView.contentInset = UIEdgeInsetsMake(value.top, value.left, value.bottom, value.right) }

        override var pitch: Double
            get() = mlnMapView.camera.pitch
            set(value) {
                val cam = mlnMapView.camera
                cam.pitch = value
                mlnMapView.setCamera(cam, animated = false)
            }

        override var minPitch: Double?
            get() = mlnMapView.minimumPitch
            set(value) { mlnMapView.minimumPitch = value ?: 0.0 }

        override var maxPitch: Double?
            get() = mlnMapView.maximumPitch
            set(value) { mlnMapView.maximumPitch = value ?: 60.0 }

        override fun ease(animateTo: Camera.AnimateTo) {
            mlnMapView.setCamera(
                animateTo.toMLNCamera(),
                withDuration = animateTo.duration.inWholeMilliseconds / 1000.0,
                animationTimingFunction = CAMediaTimingFunction.functionWithName(kCAMediaTimingFunctionEaseInEaseOut),
                completionHandler = null,
            )
        }

        override fun fly(animateTo: Camera.AnimateTo) {
            mlnMapView.flyToCamera(
                animateTo.toMLNCamera(),
                withDuration = animateTo.duration.inWholeMilliseconds / 1000.0,
                completionHandler = null,
            )
        }

        override fun stopAnimation() {
            mlnMapView.setCamera(mlnMapView.camera, animated = false)
        }

        private fun Camera.AnimateTo.toMLNCamera(): MLNMapCamera {
            val current = mlnMapView.camera
            val newCenter = center?.toCLCoordinate() ?: current.centerCoordinate
            val newBearing = bearing ?: current.heading
            val newPitch = pitch ?: current.pitch
            val newAltitude = if (zoom != null) {
                val size = mlnMapView.bounds.useContents { CGSizeMake(size.width, size.height) }
                MLNAltitudeForZoomLevel(zoom, newPitch, newCenter.useContents { latitude }, size)
            } else {
                current.altitude
            }
            return MLNMapCamera.cameraLookingAtCenterCoordinate(
                newCenter,
                fromDistance = newAltitude,
                pitch = newPitch,
                heading = newBearing,
            )
        }
    }

    // ---- Style delegate ----

    internal val emptySourceId = Uuid.random()

    private val mapDelegate = object : NSObject(), MLNMapViewDelegateProtocol {
        override fun mapView(mapView: MLNMapView, didFinishLoadingStyle: MLNStyle) {
            isStyleLoaded = true
            loadedImages.forEach { (id, image) ->
                didFinishLoadingStyle.setImage(image, forName = id.toString())
            }
            didFinishLoadingStyle.addSource(
                MLNShapeSource(
                    emptySourceId.toString(),
                    shapeFromGeoJson("""{"type":"FeatureCollection","features":[]}"""),
                    null,
                )
            )
            sources.forEach { (id, source) ->
                didFinishLoadingStyle.addSource(
                    MLNShapeSource(id.toString(), shapeFromGeoJson(source.data), null)
                )
            }
            layers.forEach { (id, layer) ->
                didFinishLoadingStyle.addLayer(toMLNLayer(id.toString(), layer, didFinishLoadingStyle))
            }
        }
    }

    // ---- Tap handler ----

    private val tapTarget = object : NSObject() {
        @ObjCAction
        fun handleTap(sender: UITapGestureRecognizer) {
            if (sender.state != UIGestureRecognizerStateEnded) return
            val point = sender.locationInView(mlnMapView)
            val handled = handleSourceLayerClicks { layerIds ->
                @Suppress("UNCHECKED_CAST")
                (mlnMapView.visibleFeaturesAtPoint(
                    point,
                    inStyleLayersWithIdentifiers = layerIds.map { it.toString() }.toSet(),
                ) as List<MLNFeatureProtocol>).map { feature ->
                    feature.attributes.takeIf { it.isNotEmpty() }?.toJsonObject()
                }
            }
            if (!handled) {
                val coord = mlnMapView.convertPoint(point, toCoordinateFromView = mlnMapView)
                clickListeners.forEach { it(coord.toGeoCoordinate()) }
            }
        }
    }

    // ---- Init ----

    init {
        mlnMapView.delegate = mapDelegate

        if (!interactive) {
            mlnMapView.scrollEnabled = false
            mlnMapView.zoomEnabled = false
            mlnMapView.rotateEnabled = false
            mlnMapView.pitchEnabled = false
        }

        if (disableAttribution != null) {
            mlnMapView.attributionButton.hidden = true
            mlnMapView.logoView.hidden = true
        }
        mlnMapView.compassView.hidden = true

        style = Style.Demo

        val tapRecognizer = UITapGestureRecognizer(tapTarget, sel_registerName("handleTap:"))
        mlnMapView.addGestureRecognizer(tapRecognizer)

        onRemove { mlnMapView.delegate = null }
    }
}

actual fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit) {
    clickListeners += callback
}

// ---- Helpers ----

private fun MapView.toMLNLayer(id: String, layer: Layer<*>?, style: MLNStyle): MLNStyleLayer {
    if (layer == null) {
        val emptySource = style.sourceWithIdentifier(emptySourceId.toString())!!
        return MLNSymbolStyleLayer(id, emptySource)
    }

    val json = layer.toJson()
    val type = json["type"]?.jsonPrimitive?.content ?: "symbol"
    val nativeSource = style.sourceWithIdentifier(layer.source.toString())!!

    val nativeLayer: MLNStyleLayer = when (type) {
        "background" -> MLNBackgroundStyleLayer(id)
        "circle" -> MLNCircleStyleLayer(id, nativeSource)
        "fill" -> MLNFillStyleLayer(id, nativeSource)
        "fill-extrusion" -> MLNFillExtrusionStyleLayer(id, nativeSource)
        "heatmap" -> MLNHeatmapStyleLayer(id, nativeSource)
        "hillshade" -> MLNHillshadeStyleLayer(id, nativeSource)
        "line" -> MLNLineStyleLayer(id, nativeSource)
        "raster" -> MLNRasterStyleLayer(id, nativeSource)
        "symbol" -> MLNSymbolStyleLayer(id, nativeSource)
        else -> throw IllegalArgumentException("Unsupported layer type '$type'")
    }

    layer.minZoom?.let { nativeLayer.minimumZoomLevel = it.toFloat() }
    layer.maxZoom?.let { nativeLayer.maximumZoomLevel = it.toFloat() }

    for (prop in layer.properties) {
        val value = prop.value ?: continue
        when {
            prop.group == Layer.PropertyGroup.Filter ->
                (nativeLayer as? MLNVectorStyleLayer)?.predicate =
                    NSPredicate.predicateWithMLNJSONObject(value.json.toFoundation()!!)
            prop.name == "visibility" ->
                nativeLayer.visible = (value.json as? JsonPrimitive)?.content != "none"
            else ->
                nativeLayer.setValue(
                    NSExpression.expressionWithMLNJSONObject(value.json.toFoundation()!!),
                    forKey = prop.cocoaName,
                )
        }
    }

    return nativeLayer
}

private fun shapeFromGeoJson(json: String): MLNShape? = memScoped {
    val data = (json as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    val error = alloc<ObjCObjectVar<NSError?>>()
    MLNShape.shapeWithData(data, NSUTF8StringEncoding, error.ptr)
}

@Suppress("UNCHECKED_CAST")
private fun Map<*, *>.toJsonObject(): JsonObject = JsonObject(
    mapNotNull { (key, value) ->
        val k = key as? String ?: return@mapNotNull null
        k to (value.toJsonElement() ?: return@mapNotNull null)
    }.toMap()
)

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> toJsonObject()
    is List<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}

fun CValue<CLLocationCoordinate2D>.toGeoCoordinate() =
    useContents { GeoCoordinate(latitude, longitude) }

fun GeoCoordinate.toCLCoordinate(): CValue<CLLocationCoordinate2D> =
    CLLocationCoordinate2DMake(latitude, longitude)
