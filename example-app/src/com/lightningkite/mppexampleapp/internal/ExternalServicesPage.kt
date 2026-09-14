package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Routable("external-services")
object ExternalServicesPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            padded.col {
                h1 { content = "This screen demonstrates various some external access." }
            }

            titledSection(
                titleSetup = { content = "Open tab" },
                content = {
                    subtext("Using `externalLink {}`")
                    card.externalLink {
                        text { content = "Open (https://google.com)" }
                        to = "https://google.com"
                    }

                    card.externalLink {
                        text { content = "openTab (mail)" }
                        to = "mailto:joseph@lightningkite.com"
                    }
                    card.externalLink {
                        text { content = "openTab (phone)" }
                        to = "tel:8013693729"
                    }
                }
            )
//            titledSection(
//                titleSetup = { content = "Open tab" },
//                content = {
//                    subtext("Using `context.openTab()`")
//
//                    card.button {
//                        text { content = "Open (https://google.com)" }
//                        onClick { context.openTab("https://google.com") }
//                    }
//
//                    card.button {
//                        text { content = "openTab (mail)" }
//                        onClick { context.openTab("mailto:joseph@lightningkite.com") }
//                    }
//                    card.button {
//                        text { content = "openTab (phone)" }
//                        onClick { context.openTab("tel:8013693729") }
//                    }
//                }
//            )

            titledSection(
                titleSetup = { content = "Open Map" },
                content = {
                    card.button {
                        text("Open Map (Null Island)")
                        onClick { context.openMap(latitude = 0.0, longitude = 0.0, label = "Null Island") }
                    }
                }
            )
            titledSection(
                titleSetup = { content = "Open Event" },
                content = {
                    card.button {
                        text("Open Event")
                        onClick {
                            context.openEvent(
                                title = "Test Event",
                                description = "This is a test event from the KiteUI Tester app.",
                                location = "255 S 300 W Logan, UT 84321",
                                start = Clock.System.now().plus(1.hours)
                                    .toLocalDateTime(TimeZone.currentSystemDefault()),
                                end = Clock.System.now().plus(2.hours).toLocalDateTime(TimeZone.currentSystemDefault()),
                                zone = TimeZone.currentSystemDefault()
                            )
                        }
                    }
                }
            )
            titledSection(
                titleSetup = { content = "Share" },
                content = {
                    row {
                        card.button {
                            text("Share KiteUI")
                            onClick {
                                context.share(
                                    "Cool Thing",
                                    "Check out this cool thing!",
                                    "https://github.com/lightningkite/kiteui"
                                )
                            }
                        }
                        card.button {
                            text("Share image")
                            onClick {
                                val blob =
                                    fetch("https://static.wikia.nocookie.net/fzero/images/d/da/Captain_Falcon_SSBU.png").blob()
                                context.share(listOf("Captain_Falcon.png" to blob))
                            }
                        }
                    }
                }
            )

            titledSection(
                titleSetup = { content = "Download" },
                content = {
                    scrollingHorizontally.row {
                        card.button {
                            text { content = "Image" }
                            onClick {
                                context.download(
                                    "test.jpg",
                                    "https://picsum.photos/200/300",
                                    DownloadLocation.Downloads
                                )
                            }
                        }
                        card.button {
                            text { content = "Image to Gallery" }
                            onClick {
                                context.download(
                                    "test.jpg",
                                    "https://picsum.photos/200/300",
                                    DownloadLocation.Pictures
                                )
                            }
                        }

                        card.button {
                            text { content = "CSV file" }
                            onClick {
                                context.download(
                                    "kiteui_example.csv",
                                    """
                                name,phone
                                Joseph Ivie,8013693729
                                Dan Ostler,9876543210,
                                Brady Svedin,4632180951
                            """.trimIndent().toBlob("text/csv; charset=utf-8; header=present")
                                )
                                context.toast { text { content = "Check downloads for file kiteui_example.csv" } }
                            }
                        }
                        card.button {
                            onClick {
                                try {
                                    context.download("kiteui_example.txt", "Hello from KiteUI!".toBlob())
                                    context.toast { text { content = "Check downloads for file kiteui_example.txt" } }
                                } catch (e: Exception) {
                                    e.printStackTrace2()
                                }

                            }
                            text("TXT file")
                        }
                    }
                })

            titledSection(
                titleSetup = { content = "Request Files" },
                content = {
                    row {
                        card.button {
                            text { content = "SingleFile (requestFile)" }
                            onClick {
                                context.requestFile(listOf("*/*"))
                            }
                        }

                        card.button {
                            text { content = "Multiple Files" }
                            onClick {
                                context.requestFiles(listOf("*/*"))
                            }
                        }
                    }

                    val photo = Signal<FileReference?>(null)
                    row {
                        card.button {
                            text { content = "Single Image (requestFile(listOf(\"image/*\"))" }
                            onClick {
                                photo.value = context.requestFile(listOf("image/*"))
                            }
                        }

                        card.button {
                            text { content = "Multiple Images" }
                            onClick {
                                photo.value = context.requestFiles(listOf("image/*")).firstOrNull()
                            }
                        }
                    }

                    row {
                        card.button {
                            text { content = "requestCaptureSelf" }
                            onClick {
                                photo.value = context.requestCaptureSelf(listOf("image/*"))
                            }
                        }

                        card.button {
                            text { content = "requestCaptureEnvironment" }
                            onClick {
                                photo.value =
                                    context.requestCaptureEnvironment(listOf("image/*"))
                            }
                        }
                    }

                    sizeConstraints(height = 30.rem).image {
                        ::shown { photo() != null }
                        ::source { photo()?.let { ImageLocal(it) } }
                        scaleType = ImageScaleType.Crop
                    }
                })


            titledSection(
                titleSetup = { content = "Geolocation" },
                content = {
                    card.button {
                        onClick {
                            try {
                                val location = context.getCurrentPosition()
                                context.dialog {
                                    col {
                                        text("Location")
                                        space()
                                        text {
                                            ::content { "Latitude: ${location.latitude}" }
                                            ::content { "Longitude: ${location.longitude}" }
                                        }
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace2()
                                throw Exception("This probably isn't working because permissions haven't been granted")
                            }
                        }
                        row {
                            text("Get Current Location")
                            expanding.space()
                            icon { source = Icon.home }
                        }
                    }
                }
            )
            space(4.0)
//            row {
//                textField { content bind clip }
//                card.button {
//                    row {
//                        icon {
//                            source = Icon.copy
//                            description = "Copy text to clipboard"
//                        }
//
//                        text {content = "setClipboardText"}
//                    }
//
//
//                    onClick{context.setClipboardText(clip.await())}
//                }
//            }
        }
    }
}