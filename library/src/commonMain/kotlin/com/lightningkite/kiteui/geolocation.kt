package com.lightningkite.kiteui

public expect object Geolocation {
    suspend fun getCurrentPosition(

    ): GeolocationResult
}
data class GeolocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyInMeters: Double,
)