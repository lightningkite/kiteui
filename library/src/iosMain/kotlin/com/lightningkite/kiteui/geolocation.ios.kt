package com.lightningkite.kiteui

actual object Geolocation {
    actual suspend fun getCurrentPosition(): GeolocationResult = TODO()
}