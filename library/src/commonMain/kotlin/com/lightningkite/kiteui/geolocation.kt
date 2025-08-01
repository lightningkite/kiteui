package com.lightningkite.kiteui

public expect object Geolocation {
    public suspend fun getCurrentPosition(

    ): GeolocationResult
}
public data class GeolocationResult(
    public val latitude: Double,
    public val longitude: Double,
    public val accuracyInMeters: Double,
)