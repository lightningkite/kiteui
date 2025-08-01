package com.lightningkite.kiteui

import kotlinx.coroutines.suspendCancellableCoroutine

@InternalKiteUi
public actual object Geolocation {
    public actual suspend fun getCurrentPosition(): GeolocationResult {
        return suspendCancellableCoroutine<GeolocationResult> {  }
    }
}