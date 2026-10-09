package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.map.Map
import com.lightningkite.kiteui.map.mapView
import com.lightningkite.kiteui.map.mark
import com.lightningkite.kiteui.map.markMany
import com.lightningkite.kiteui.map.onClick
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.atBottomStart
import com.lightningkite.kiteui.views.atTopStart
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.icon
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrollingHorizontally
import com.lightningkite.kiteui.views.direct.unpadded
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.modify
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Routable("/map-view-test")
object MapViewTestPage : Page {
    override val title: Reactive<String> = Constant("Map View Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        unpadded.frame {
            val useBaseDemoTiles = Signal(true)
            val markerPosition = Signal(GeoCoordinate(0.0, 0.0))
            val markerCollection = Signal(listOf<Pair<Int, GeoCoordinate>>())

            val map = mapView {
                ::style style@{
                    if (useBaseDemoTiles()) return@style Map.Style.Demo
                    if (Platform.current == Platform.Web) return@style Map.Style.GlobeDemo
                    // MapLibre on mobile currently (unfortunately) only supports the Mercator projection, so I'm using a separate debug style instead
                    Map.Style.Debug
                }

                mark(markerPosition)
                markMany(markerCollection, { it.first }) { it.second }

                onClick { markerPosition.value = it }
            }

            (if (Platform.current == Platform.Web) atTopStart else atBottomStart).padded.scrollingHorizontally.row {
                ignoreInteraction = true

                card.button {
                    icon { ::source { if (useBaseDemoTiles() && Platform.current == Platform.Web) Icon.globe else Icon.map } }
                    onClick { useBaseDemoTiles.modify { !it } }
                }


                card.button {
                    icon(Icon.globeLocationPin, "Test Update Marker")
                    onClick {
                        val newPosition = Random.nextGeoCoordinate()
                        markerPosition.value = newPosition
                        map.camera.center = newPosition
                    }
                }

                card.button {
                    icon(Icon.travelExplore, "Test Ease To")
                    onClick { launch { map.camera.easeTo(Random.nextEaseToOptions()) } }
                }

                card.button {
                    icon(Icon.travel, "Test Fly To")
                    onClick { launch { map.camera.flyTo(Random.nextFlyToOptions()) } }
                }

                val multipartFlyToJob = Signal<Job?>(null)
                card.button {
                    icon(Icon.planeContrails, "Test Multipart flyTo")
                    onClick {
                        multipartFlyToJob.value = launch {
                            while (true) {
                                val options = Random.nextFlyToOptions()
                                map.camera.flyTo(options)
                                delay(options.duration)
                            }
                        }
                    }
                }

                card.button {
                    icon(Icon.airplaneModeInactive, "Cancel Animation")
                    onClick { map.camera.stopAnimation(); multipartFlyToJob()?.cancel() }
                }

                card.button {
                    icon(Icon.addLocationAlt, "Add Marker")
                    onClick { markerCollection.modify { it + (it.size to Random.nextGeoCoordinate()) } }
                }

                card.button {
                    icon(Icon.wrongLocation, "Remove Marker")
                    onClick { markerCollection.modify { it.drop(1) } }
                }

                card.button {
                    icon(Icon.collapse, "Test Constraints")
                    onClick {
                        map.camera.minZoom = Random.nextDouble(0.0, 8.0)
                        map.camera.maxZoom = Random.nextDouble(12.0, 22.0)
                    }
                }
            }
        }
    }
}

fun Random.nextGeoCoordinate() = GeoCoordinate(nextDouble(-90.0, 90.0), nextDouble(-180.0, 180.0))
fun Random.nextEaseToOptions() = Map.EaseToOptions(
    center = nextGeoCoordinate(),
    zoom = nextDouble(0.0, 5.0),
    bearing = nextDouble(0.0, 360.0),
    pitch = nextDouble(0.0, 60.0),
    duration = nextDouble(0.5, 3.0).seconds,
)

fun Random.nextFlyToOptions() = Map.FlyToOptions(
    center = nextGeoCoordinate(),
    zoom = nextDouble(3.0, 8.0),
    bearing = nextDouble(0.0, 360.0),
    pitch = nextDouble(0.0, 60.0),
    duration = nextDouble(2.0, 4.0).seconds,
)

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

val Icon.Companion.travel
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("m393-119-95-179-180-96 59-59 148 27 122-121-327-139 72-72 396 69 133-133q21-21 50.5-21t50.5 21q21 21 21 50.5T822-721L689-588l69 396-72 72-139-327-121 122 26 147-59 59Z")
    )

val Icon.Companion.globeLocationPin
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q79 0 149.5 29T754-772q54 50 87 118.5T879-506q-15-9-30.5-15.5T816-533q-14-90-69.5-160.5T607-799v18q0 35-24 61t-59 26h-87v87q0 17-13.5 28T393-568h-83v88h297q-32 31-49.5 71T540-324q0 75 31.5 115.5T650-118q-40 19-82.5 28.5T480-80Zm-43-61v-82q-35 0-59-26t-24-61v-44L149-559q-5 20-7 39.5t-2 39.5q0 130 84.5 227T437-141Zm365.5-136.5Q820-295 820-320t-17-42.5Q786-380 761-380q-26 0-43.5 17.5T700-320q0 25 17.5 42.5T760-260q25 0 42.5-17.5ZM760-80q-3 0-16-11l-4-7q-22-38-55.5-67.5T627-232q-14-20-20.5-43.5T600-324q0-66 47-111t113-45q66 0 113 45t47 111q0 25-6.5 48.5T893-232q-24 37-57.5 66.5T780-98l-4 7q-2 5-6.5 8t-9.5 3Z")
    )

val Icon.Companion.addLocationAlt
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M480-80Q319-217 239.5-334.5T160-552q0-150 96.5-239T480-880q17 0 32.5 1.5T544-874v62q-15-4-31-6t-33-2q-109.42 0-184.71 75.1Q220-669.79 220-552q0 75 65 173.5T480-159q133-121 196.5-219.5T740-552q0-8-.5-16t-1.5-16h61q1 8 1 16v16q0 100-79.5 217.5T480-80Zm49.5-430.59q20.5-20.59 20.5-49.5t-20.59-49.41q-20.59-20.5-49.5-20.5t-49.41 20.59q-20.5 20.59-20.5 49.5t20.59 49.41q20.59 20.5 49.5 20.5t49.41-20.59ZM480-560Zm252-84h60v-128h128v-60H792v-128h-60v128H604v60h128v128Z")
    )

val Icon.Companion.wrongLocation
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("M480-80Q319-217 239.5-334.5T160-552q0-150 96.5-239T480-880q16 0 32 2t32 5v61q-15.67-4-31.33-6-15.67-2-32.67-2-109.42 0-184.71 75.1Q220-669.79 220-552q0 75 65 173.5T480-159q133-121 196.5-219.5T740-552q0-8-.5-16t-1.5-16h60q1 8 1.5 16t.5 16q0 100-79.5 217.5T480-80Zm0-433Zm195-139 84-84 84 84 42-42-84-84 84-84-42-42-84 84-84-84-42 42 84 84-84 84 42 42ZM529.5-510.59q20.5-20.59 20.5-49.5t-20.59-49.41q-20.59-20.5-49.5-20.5t-49.41 20.59q-20.5 20.59-20.5 49.5t20.59 49.41q20.59 20.5 49.5 20.5t49.41-20.59Z")
    )

val Icon.Companion.planeContrails
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("m371-80-42-43 173-173 43 42L371-80Zm207 0-43-42 154-154 42 43L578-80ZM123-534l-43-43 153-153 43 43-153 153Zm0 206-43-42 174-174 43 42-174 174Zm648-26-99-247-97 98 20 97-34 34-68-121-121-68 34-34 98 20 97-97-247-99 45-42 297 47 98-99q7-7 16-10.5t19-3.5q10 0 19 3.5t17 11.5q8 7 11.5 16t3.5 18q0 10-4 19t-12 17l-98 98 47 297-42 45Z")
    )

val Icon.Companion.airplaneModeInactive
    get() = Icon(
        1.5.rem,
        1.5.rem,
        0,
        -960,
        960,
        960,
        listOf("m880-288-232-94-239-239v-188q0-29 21-50t50-21q29 0 50 21t21 50v188l329 231v102ZM480-139 285-80v-83l124-86v-172L80-288v-102l230-162L56-806l42-42L848-97l-42 41-255-255v62l123 86v83l-194-59Z")
    )