package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.GeolocationResult
import com.lightningkite.kiteui.getCurrentPosition
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

// by Claude
@Routable("geolocation-test")
object GeolocationTestPage : Page {
    override val title: Reactive<String> get() = Constant("Geolocation Test")

    // Prefixed to avoid shadowing RViewHelper.loading (which is a read-only ReactiveValue)
    val geoResult = Signal<GeolocationResult?>(null)
    val geoError = Signal<String?>(null)
    val geoLoading = Signal(false)

    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            h1 { content = "Geolocation Test" }

            button {
                text {
                    ::content { if (geoLoading()) "Requesting location..." else "Get Current Position" }
                }
                ::enabled { !geoLoading() }
                onClick {
                    geoLoading.value = true
                    geoError.value = null
                    geoResult.value = null
                    val ctx = context
                    try {
                        geoResult.value = ctx.getCurrentPosition()
                    } catch (e: Exception) {
                        geoError.value = e.message ?: "Unknown error"
                    } finally {
                        geoLoading.value = false
                    }
                }
            }

            text { ::content { geoResult()?.let { "Latitude:  ${it.latitude}" } ?: "" } }
            text { ::content { geoResult()?.let { "Longitude: ${it.longitude}" } ?: "" } }
            text { ::content { geoResult()?.let { "Accuracy:  ${it.accuracyInMeters} m" } ?: "" } }
            danger.text { ::content { geoError() ?: "No Error" } }
        }
    }
}
