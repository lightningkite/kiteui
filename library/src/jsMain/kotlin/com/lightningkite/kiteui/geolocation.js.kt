package com.lightningkite.kiteui

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.js.json

@InternalKiteUi
public actual object Geolocation {
    public actual suspend fun getCurrentPosition(): GeolocationResult {
        return suspendCancellableCoroutine<GeolocationResult> { cont ->
            window.navigator.asDynamic().geolocation.getCurrentPosition(
                { result: dynamic ->
                    cont.resume(GeolocationResult(
                        latitude = result.coords.latitude,
                        longitude = result.coords.longitude,
                        accuracyInMeters = result.coords.accuracy,
                    ), null)
                },
                { error: dynamic ->
                    cont.resumeWithException(Exception("Geolocation error: $error"))
                },
                json("enableHighAccuracy" to true, "timeout" to 10000, "maximumAge" to 0)
            )
        }
    }
}