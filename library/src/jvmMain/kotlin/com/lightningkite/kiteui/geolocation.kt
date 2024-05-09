package com.lightningkite.kiteui

import ViewWriter

actual fun ViewWriter.geolocate(onFixed: (GeolocationResult) -> Unit): Unit = TODO()

actual fun ViewWriter.watchGeolocation(onUpdated: (GeolocationResult) -> Unit): Unit = TODO()
