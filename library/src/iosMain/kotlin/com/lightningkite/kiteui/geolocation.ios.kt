package com.lightningkite.kiteui

@InternalKiteUi
public actual object Geolocation {
    public actual suspend fun getCurrentPosition(): GeolocationResult = TODO()
}