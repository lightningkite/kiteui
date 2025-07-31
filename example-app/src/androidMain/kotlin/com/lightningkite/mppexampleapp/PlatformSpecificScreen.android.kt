package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.signal.Property
import com.lightningkite.signal.bind
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources

public actual fun ViewWriter.platformSpecific(): ViewModifiable {
    return col {
        text("Lookie here at this icon:")
        val visible = Property(false)
        switch { checked bind visible }
        expanding - zoomableImage {
            source = Resources.imagesSolera
        }
    }
}