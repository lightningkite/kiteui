package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun ViewWriter.platformSpecific() {
    col {
        text("TEST")
    }
}
