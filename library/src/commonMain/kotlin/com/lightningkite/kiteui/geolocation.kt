package com.lightningkite.kiteui

import ViewWriter

data class GeolocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
)

expect fun ViewWriter.geolocate(onFixed: (GeolocationResult) -> Unit)
expect fun ViewWriter.watchGeolocation(onUpdated: (GeolocationResult) -> Unit)
