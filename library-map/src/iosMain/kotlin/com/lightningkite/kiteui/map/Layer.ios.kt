package com.lightningkite.kiteui.map

import cocoapods.MapLibre.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import platform.Foundation.*

private val Layer<*>.nativeLayerOrNull: MLNStyleLayer?
    get() = mapView.mlnMapView.style?.layerWithIdentifier(id.toString())

/** The live layer. Only valid while [Layer.isLive]. */
private val Layer<*>.nativeLayer: MLNStyleLayer get() = nativeLayerOrNull!!

internal actual val Layer<*>.isNativeLayerAdded: Boolean get() = nativeLayerOrNull != null

internal actual fun Layer<*>.writeLiveProperty(prop: Layer<*>.Property<*>, value: JsonElement?) {
    when {
        prop.group == Layer.PropertyGroup.Filter ->
            (nativeLayer as? MLNVectorStyleLayer)?.predicate =
                value?.let { NSPredicate.predicateWithMLNJSONObject(it.toFoundation()!!) }
        prop.name == "visibility" ->
            nativeLayer.visible = (value as? JsonPrimitive)?.content != "none"
        else ->
            nativeLayer.setValue(
                value?.let { NSExpression.expressionWithMLNJSONObject(it.toFoundation()!!) },
                forKey = prop.cocoaName,
            )
    }
}

internal actual fun Layer<*>.setLiveMinZoom(value: Double?) {
    nativeLayer.minimumZoomLevel = value?.toFloat() ?: Float.NEGATIVE_INFINITY
}

internal actual fun Layer<*>.setLiveMaxZoom(value: Double?) {
    nativeLayer.maximumZoomLevel = value?.toFloat() ?: Float.POSITIVE_INFINITY
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun JsonElement.toFoundation(): Any? = NSJSONSerialization.JSONObjectWithData(
    (toString() as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!,
    NSJSONReadingAllowFragments,
    null,
)
