package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer

@Routable("external-services")
object ExternalServicesPage : Page {
    override val title: Reactive<String>
        get() = super.title
    val image = Signal<ImageSource?>(null)
    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            padded.col {
                h1 { content = "This screen demonstrates various some external access." }
            //                text { content = "Note the use of the multi-layer 'Reactive' in `fetching`." }
            }

            row {
                button {
                    text { content = "openTab" }
                    onClick { context.openTab("https://google.com") }
                }

                button {
                    text { content = "openTab (mail)" }
                    onClick { context.openTab("mailto:joseph@lightningkite.com") }
                }
                button {
                    text { content = "openTab (phone)" }
                    onClick { context.openTab("tel:8013693729") }
                }
            }
            row {
                externalLink {
                    text { content = "openTab" }
                    to = "https://google.com"
                }

                externalLink {
                    text { content = "openTab (mail)" }
                    to = "mailto:joseph@lightningkite.com"
                }
                externalLink {
                    text { content = "openTab (phone)" }
                    to = "tel:8013693729"
                }
            }

            scrollingHorizontally.row {
                button {
                    text("Open Map")
                    onClick { context.openMap(latitude = 0.0, longitude = 0.0, label = "Null Island") }
                }
                button {
                    text("Open Event")
                    onClick {
                        context.openEvent(
                            title = "Test Event",
                            description = "This is a test event from the KiteUI Tester app.",
                            location = "255 S 300 W Logan, UT 84321",
                            start = Clock.System.now().plus(1.hours).toLocalDateTime(TimeZone.currentSystemDefault()),
                            end = Clock.System.now().plus(2.hours).toLocalDateTime(TimeZone.currentSystemDefault()),
                            zone = TimeZone.currentSystemDefault()
                        )
                    }
                }
                button {
                    text("Download")
                    onClick {
                        context.download(
                            "yes.png",
                            "https://static.wikia.nocookie.net/fzero/images/d/da/Captain_Falcon_SSBU.png"
                        )
                    }
                }
                button {
                    text("Share")
                    onClick {
                        context.share(
                            "Cool Thing",
                            "Check out this cool thing!",
                            "https://github.com/lightningkite/kiteui"
                        )
                    }
                }
                button {
                    text("Share image")
                    onClick {
                        val blob =
                            fetch("https://static.wikia.nocookie.net/fzero/images/d/da/Captain_Falcon_SSBU.png").blob()
                        context.share(listOf("Captain_Falcon.png" to blob))
                    }
                }
            }

            scrollingHorizontally.row {
                button {
                    text { content = "download image" }
                    onClick {
                        ExternalServices.download(
                            "test.jpg",
                            "https://picsum.photos/200/300",
                            DownloadLocation.Downloads
                        )
                    }
                }
                button {
                    text { content = "download gallery image" }
                    onClick {
                        ExternalServices.download(
                            "test.jpg",
                            "https://picsum.photos/200/300",
                            DownloadLocation.Pictures
                        )
                    }
                }

                button {
                    text { content = "download csv" }
                    onClick {
                        ExternalServices.download(
                            "file.csv",
                            """
                                name,phone
                                Joseph Ivie,8013693729
                                Dan Ostler,9876543210,
                                Brady Svedin,4632180951
                            """.trimIndent().toBlob("text/csv; charset=utf-8; header=present")
                        )
                    }
                }
            }

            row {

                button {
                    text { content = "requestFile" }
                    onClick {
                        println(context.requestFile(listOf("*/*")))
                    }
                }

                button {
                    text { content = "requestFiles" }
                    onClick {
                        println(context.requestFiles(listOf("*/*")))
                    }
                }
            }

            row {
                button {
                    text { content = "requestFile image" }
                    onClick {
                        image.value = context.requestFile(listOf("image/*"))?.let { ImageLocal(it) }
                    }
                }

                button {
                    text { content = "requestFiles image" }
                    onClick {
                        image.value =
                            context.requestFiles(listOf("image/*"))?.firstOrNull()?.let { ImageLocal(it) }
                    }
                }
            }

            row {
                button {
                    text { content = "requestCaptureSelf" }
                    onClick {
                        image.value = context.requestCaptureSelf(listOf("image/*"))?.let { ImageLocal(it) }
                    }
                }

                button {
                    text { content = "requestCaptureEnvironment" }
                    onClick {
                        image.value =
                            context.requestCaptureEnvironment(listOf("image/*"))?.let { ImageLocal(it) }
                    }
                }
            }

            sizeConstraints(height = 30.rem).image {
                ::source { image.invoke() }
                scaleType = ImageScaleType.Crop
            }
//            row {
//                textField { content bind clip }
//                button {
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