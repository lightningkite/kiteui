package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.map.Map
import com.lightningkite.kiteui.map.mapView
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.atTopStart
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.icon
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.unpadded
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.modify
import com.lightningkite.services.data.GeoCoordinate
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Routable("/map-view-test")
object MapViewTestPage : Page {
    override val title: Reactive<String> = Constant("Map View Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        unpadded.frame {
            val useBaseDemoTiles = Signal(true)

            val map = mapView {
                ::style style@{
                    if (useBaseDemoTiles()) return@style Map.Style.Demo
                    if (Platform.current == Platform.Web) return@style Map.Style.GlobeDemo
                    // MapLibre on mobile currently (unfortunately) only supports the Mercator projection, so I'm using a separate debug style instead
                    Map.Style.Debug
                }
                center = GeoCoordinate(90.0, 90.0)
            }

            atTopStart.padded.row {
                ignoreInteraction = true

                card.button {
                    icon { ::source { if (useBaseDemoTiles() && Platform.current == Platform.Web) Icon.globe else Icon.map } }
                    onClick { useBaseDemoTiles.modify { !it } }
                }

                card.button {
                    icon(Icon.travelExplore, "Test Fly To")
                    onClick {
                        map.camera.flyTo(
                            Map.FlyToOptions(
                                center = GeoCoordinate(
                                    Random.nextDouble(-90.0, 90.0),
                                    Random.nextDouble(-180.0, 180.0)
                                ),
                                zoom = Random.nextDouble(0.0, 5.0),
                                duration = Random.nextDouble(0.5, 3.0).seconds,
                            )
                        )
                    }
                }
            }
        }
    }
}

val Icon.Companion.globe
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M480.27-80q-82.74 0-155.5-31.5Q252-143 197.5-197.5t-86-127.34Q80-397.68 80-480.5t31.5-155.66Q143-709 197.5-763t127.34-85.5Q397.68-880 480.5-880t155.66 31.5Q709-817 763-763t85.5 127Q880-563 880-480.27q0 82.74-31.5 155.5Q817-252 763-197.68q-54 54.31-127 86Q563-80 480.27-80Zm-.27-60q142.38 0 241.19-99.5T820-480v-13q-6 26-27.41 43.5Q771.19-432 742-432h-80q-33 0-56.5-23.5T582-512v-40H422v-80q0-33 23.5-56.5T502-712h40v-22q0-16 13.5-40t30.5-29q-25-8-51.36-12.5Q508.29-820 480-820q-141 0-240.5 98.81T140-480h150q66 0 113 47t47 113v40H330v105q34 17 71.7 26t78.3 9Z")
    )

val Icon.Companion.map
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("m612-120-263-93-179 71q-17 9-33.5-1T120-173v-558q0-13 7.5-23t19.5-15l202-71 263 92 178-71q17-8 33.5 1.5T840-788v565q0 11-7.5 19T814-192l-202 72Zm-34-75v-505l-196-66v505l196 66Zm60 0 142-47v-512l-142 54v505Zm-458-12 142-54v-505l-142 47v512Zm458-493v505-505Zm-316-66v505-505Z")
    )

val Icon.Companion.travelExplore
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M458-81q-79-4-148-37t-120-86.5Q139-258 109.5-329T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q149 0 259 94t135 236h-61q-17-84-71-150t-135-99v18q0 35-24 61t-59 26h-87v87q0 17-13.5 28T393-568h-83v88h110v125h-67L149-559q-5 20-7 39.5t-2 39.5q0 135 91 233t227 106v60Zm392-26L716-241q-21 15-45.5 23t-50.5 8q-71 0-120.5-49.5T450-380q0-71 49.5-120.5T620-550q71 0 120.5 49.5T790-380q0 26-8.5 50.5T759-283l134 133-43 43ZM698-302q32-32 32-78t-32-78q-32-32-78-32t-78 32q-32 32-32 78t32 78q32 32 78 32t78-32Z")
    )