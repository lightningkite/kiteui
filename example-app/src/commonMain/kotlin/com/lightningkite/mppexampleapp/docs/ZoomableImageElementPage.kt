package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.random.Random

@Routable("docs/zoomable-image")
object ZoomableImageElementPage: DocPage {
    override val covers: List<String> = listOf("image", "Image", "zoomable")

    override fun ViewWriter.render(): ViewModifiable = run {
        zoomableImage {
            source = ImageRemote("https://picsum.photos/seed/starter/2048/2048")
        }
    }
}