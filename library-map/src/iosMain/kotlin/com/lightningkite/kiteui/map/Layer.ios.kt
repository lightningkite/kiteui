package com.lightningkite.kiteui.map

import cocoapods.MapLibre.MLNStyleLayer
import cocoapods.MapLibre.MLNVectorStyleLayer
import cocoapods.MapLibre.expressionWithMLNJSONObject
import cocoapods.MapLibre.predicateWithMLNJSONObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import platform.Foundation.NSExpression
import platform.Foundation.NSJSONReadingAllowFragments
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSPredicate
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue

private val Layer<*>.nativeLayerOrNull: MLNStyleLayer?
    get() = mapView.mlnMapView.style?.layerWithIdentifier(id.toString())

/** The live layer. Only valid while [Layer.isLive]. */
private val Layer<*>.nativeLayer: MLNStyleLayer get() = nativeLayerOrNull!!

internal actual val Layer<*>.isNativeLayerAdded: Boolean get() = nativeLayerOrNull != null

internal actual fun Layer<*>.writeNativeProperty(prop: Layer<*>.Property<*>, value: JsonElement?) {
    when {
        // iOS represents filters as predicates.
        prop.group == PropertyGroup.Filter -> (nativeLayer as MLNVectorStyleLayer).predicate =
            value?.let { NSPredicate.predicateWithMLNJSONObject(it.toFoundation()!!) }
        // iOS replaces the `visibility` property with `MLNStyleLayer.visible`.
        prop.name == "visibility" -> nativeLayer.visible = (value as? JsonPrimitive)?.content != "none"
        // The `MLN*StyleLayer` property, which iOS names differently than the style spec.
        else -> nativeLayer.setValue(value?.let { NSExpression.expressionWithMLNJSONObject(it.toFoundation()!!) }, forKey = prop.cocoaName)
    }
}

// MapLibre Native defaults to unbounded zoom ranges.
internal actual fun Layer<*>.setNativeMinZoom(value: Double?) {
    nativeLayer.minimumZoomLevel = value?.toFloat() ?: Float.NEGATIVE_INFINITY
}

internal actual fun Layer<*>.setNativeMaxZoom(value: Double?) {
    nativeLayer.maximumZoomLevel = value?.toFloat() ?: Float.POSITIVE_INFINITY
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun JsonElement.toFoundation(): Any? = NSJSONSerialization.JSONObjectWithData(
    (toString() as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!,
    NSJSONReadingAllowFragments,
    null,
)
