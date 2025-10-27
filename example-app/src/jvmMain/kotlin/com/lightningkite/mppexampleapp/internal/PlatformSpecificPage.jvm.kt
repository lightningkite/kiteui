package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.text

actual fun ViewWriter.platformSpecific(): ViewModifiable {
    return text("OK boomer")
}