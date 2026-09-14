package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual fun ElementWriter.platformSpecific(): Unit {
    text("Nothing yet")
}