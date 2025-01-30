package com.lightningkite.kiteui

import kotlinx.coroutines.suspendCancellableCoroutine

actual object Geolocation {
    actual suspend fun getCurrentPosition(): GeolocationResult {
        return suspendCancellableCoroutine<GeolocationResult> {  }
    }
}