package com.lightningkite.kiteui.views

actual suspend fun RView.driverScreenshot(): String = throw DriverActionException("Screenshots not supported on JVM SSR")
