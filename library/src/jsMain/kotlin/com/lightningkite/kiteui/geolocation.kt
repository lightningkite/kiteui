package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.browser.window

actual fun ViewWriter.geolocate(onFixed: (GeolocationResult) -> Unit) {
    window.navigator.asDynamic().geolocation.getCurrentPosition { position: GeolocationPosition ->
        onFixed(
            GeolocationResult(
                latitude = position.coords.latitude,
                longitude = position.coords.longitude,
                accuracy = position.coords.accuracy,
            )
        )
    }
}

actual fun ViewWriter.watchGeolocation(onUpdated: (GeolocationResult) -> Unit) {
    window.navigator.asDynamic().geolocation.watchPosition { position: GeolocationPosition ->
        onUpdated(
            GeolocationResult(
                latitude = position.coords.latitude,
                longitude = position.coords.longitude,
                accuracy = position.coords.accuracy,
            )
        )
    }
}

abstract external class GeolocationPosition {
    val coords: GeolocationCoordinates
}
abstract external class GeolocationCoordinates {
    val latitude: Double
    val longitude: Double
    val accuracy: Double
}
