package com.lightningkite.mppexampleapp

import ViewWriter
import com.lightningkite.kiteui.views.direct.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun ViewWriter.platformSpecific() {
    col {
        text("TEST")
    }
}
