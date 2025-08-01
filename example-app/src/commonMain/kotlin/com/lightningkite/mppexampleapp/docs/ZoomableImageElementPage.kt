package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs/zoomable-image")
public object ZoomableImageElementPage: DocPage {
    public override val covers: List<String> = listOf("image", "Image", "zoomable")

    public override fun ViewWriter.render(): ViewModifiable = run {
        zoomableImage {
            source = ImageRemote("https://picsum.photos/seed/starter/2048/2048")
        }
    }
}