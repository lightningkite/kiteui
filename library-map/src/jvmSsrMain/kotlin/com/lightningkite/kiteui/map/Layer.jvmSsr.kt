package com.lightningkite.kiteui.map

import kotlinx.serialization.json.JsonElement

// Server-side rendering has no map, so layers are never live.

internal actual val Layer<*>.isNativeLayerAdded: Boolean get() = false

internal actual fun Layer<*>.writeLiveProperty(prop: Layer<*>.Property<*>, value: JsonElement?) {}
internal actual fun Layer<*>.setLiveMinZoom(value: Double?) {}
internal actual fun Layer<*>.setLiveMaxZoom(value: Double?) {}
