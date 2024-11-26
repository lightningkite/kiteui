package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.bind
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

actual fun ViewWriter.platformSpecific() {
    col {
        text("Lookie here at this icon:")
        val visible = Property(false)
        switch { checked bind visible }
        expanding - zoomableImage {
            source = Resources.imagesSolera
        }
    }
}