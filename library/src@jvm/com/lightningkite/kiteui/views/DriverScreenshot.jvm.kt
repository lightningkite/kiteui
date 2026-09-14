package com.lightningkite.kiteui.views

public actual suspend fun Element.driverScreenshot(): String = throw DriverActionException("Screenshots not supported on JVM SSR")
