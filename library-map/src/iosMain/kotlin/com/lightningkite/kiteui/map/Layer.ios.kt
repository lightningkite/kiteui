package com.lightningkite.kiteui.map

import cocoapods.MapLibre.*
import kotlinx.cinterop.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import platform.CoreGraphics.CGVector
import platform.Foundation.*
import platform.UIKit.*

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
        prop.cocoaName == "textVariableAnchorOffset" ->
            nativeLayer.setValue(
                value?.let { anchorOffsetExpression(it) ?: NSExpression.expressionWithMLNJSONObject(it.toFoundation()!!) },
                forKey = prop.cocoaName,
            )
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

// MLNTextAnchor enum values (must match the order in MLNSymbolStyleLayer.h)
private val anchorStringToMLNValue: Map<String, ULong> = mapOf(
    "center" to 0UL,
    "left" to 1UL,
    "right" to 2UL,
    "top" to 3UL,
    "bottom" to 4UL,
    "top-left" to 5UL,
    "top-right" to 6UL,
    "bottom-left" to 7UL,
    "bottom-right" to 8UL,
)

/**
 * Converts AnchorOffsets JSON `["literal", ["anchor", [x, y], ...]]` to the NSExpression
 * MapLibre iOS expects for textVariableAnchorOffset: a constant NSArray alternating
 * NSNumber(MLNTextAnchor) and NSValue(CGVector) pairs.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun anchorOffsetExpression(json: JsonElement): NSExpression? {
    val outer = json as? JsonArray ?: return null
    if (outer.size != 2) return null
    val inner = outer[1] as? JsonArray ?: return null

    val result = NSMutableArray()
    var i = 0
    while (i + 1 < inner.size) {
        val anchorJson = inner[i] as? JsonPrimitive ?: return null
        if (!anchorJson.isString) return null
        val anchorValue = anchorStringToMLNValue[anchorJson.content] ?: return null

        val offsetJson = inner[i + 1] as? JsonArray ?: return null
        if (offsetJson.size != 2) return null
        val dx = (offsetJson[0] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        val dy = (offsetJson[1] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null

        result.addObject(NSNumber.numberWithUnsignedLong(anchorValue))
        result.addObject(NSValue.valueWithCGVector(cValue<CGVector> { this.dx = dx; this.dy = dy }))

        i += 2
    }

    return NSExpression.expressionForConstantValue(result)
}
