package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.map.mapView
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

@Routable("/map-view-test")
object MapViewTestPage : Page {
    override val title: Reactive<String> = Constant("Map View Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            h1("Map View")

            sizeConstraints(height = 20.rem).frame {
                mapView {

                }
            }
        }
    }
}