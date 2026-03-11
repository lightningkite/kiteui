package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*

actual fun ViewWriter.platformSpecific(): Unit {
    text("JVM Swing Platform - KiteUI Desktop App")
}
