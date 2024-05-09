package com.lightningkite.kiteui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.kiteui.views.ViewWriter

private val locationService: LocationManager by lazy {
    AndroidAppContext.applicationCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
}
@SuppressLint("MissingPermission")
actual fun ViewWriter.geolocate(onFixed: (GeolocationResult) -> Unit) {
    val location = try {
        locationService.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } catch (ex: Exception) {
        throw RuntimeException("Location permission must be called before calling ViewWriter.goelocate")
    }

    if (location != null) {
        onFixed(location.toGeolocationResult())
    } else {
        throw RuntimeException("Location not found")
    }
}

private fun Location.toGeolocationResult(): GeolocationResult {
    return GeolocationResult(latitude, longitude, this.accuracy.toDouble())
}

actual fun ViewWriter.watchGeolocation(onUpdated: (GeolocationResult) -> Unit) {
    TODO("This can be accomplished with the regular location manager, " +
            "however best practice is to use the FusedLocationProvider which is part of google play servers. " +
            "Should probably have google service functionality be part of a separate package")
}