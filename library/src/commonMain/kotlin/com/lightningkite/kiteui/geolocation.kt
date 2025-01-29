package com.lightningkite.kiteui

expect object Geolocation {
    suspend fun getCurrentPosition(

    ): GeolocationResult
}
data class GeolocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyInMeters: Double,
)