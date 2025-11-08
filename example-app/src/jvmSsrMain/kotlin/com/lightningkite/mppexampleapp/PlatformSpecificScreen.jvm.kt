package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual fun ViewWriter.platformSpecific(): ViewModifiable {
    SupervisorJob() + Dispatchers.Main
    return text("Nothing yet")
}