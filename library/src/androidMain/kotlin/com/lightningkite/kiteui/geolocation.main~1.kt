package com.lightningkite.kiteui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.lightningkite.kiteui.views.AndroidAppContext

@InternalKiteUi
public actual object Geolocation {

    private val locationService: LocationManager by lazy {
        AndroidAppContext.applicationCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private fun Location.toGeolocationResult(): GeolocationResult {
        return GeolocationResult(latitude, longitude, this.accuracy.toDouble())
    }

    @SuppressLint("MissingPermission")
    public actual suspend fun getCurrentPosition(): GeolocationResult {
        if(!AndroidAppContext.requestPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION).accepted) throw Exception("Permission not granted")
        val location = try {
            locationService.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            throw RuntimeException("Location permission must be called before calling ViewWriter.goelocate")
        }

        return location?.toGeolocationResult() ?: throw Exception("Location not found")
    }
}
