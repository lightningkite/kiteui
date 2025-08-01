package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

@Routable("external-services")
public object ExternalServicesPage : Page {
    public override val title: Readable<String>
        get() = super.title
    public val image = Property<ImageSource?>(null)
    public override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - col {
            col {
                h1 { content = "This screen demonstrates various some external access." }
//                text { content = "Note the use of the multi-layer 'Readable' in `fetching`." }
            } in padded

            row {
                button {
                    text { content = "openTab" }
                    onClick { ExternalServices.openTab("https://google.com") }
                }

                button {
                    text { content = "openTab (mail)" }
                    onClick { ExternalServices.openTab("mailto:joseph@lightningkite.com") }
                }
                button {
                    text { content = "openTab (phone)" }
                    onClick { ExternalServices.openTab("tel:8013693729") }
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

            scrollingHorizontally - row {
                button {
                    text("Open Map")
                    onClick { ExternalServices.openMap(latitude = 0.0, longitude = 0.0, label = "Null Island") }
                }
                button {
                    text("Open Event")
                    onClick { ExternalServices.openEvent(
                        title = "Test Event",
                        description = "This is a test event from the KiteUI Tester app.",
                        location = "255 S 300 W Logan, UT 84321",
                        start = Clock.System.now().plus(1.hours).toLocalDateTime(TimeZone.currentSystemDefault()),
                        end = Clock.System.now().plus(2.hours).toLocalDateTime(TimeZone.currentSystemDefault()),
                        zone = TimeZone.currentSystemDefault()
                    ) }
                }
                button {
                    text("Download")
                    onClick {
                        ExternalServices.download(
                            "yes.png",
                            "https://static.wikia.nocookie.net/fzero/images/d/da/Captain_Falcon_SSBU.png"
                        )
                    }
                }
                button {
                    text("Share")
                    onClick {
                        ExternalServices.share("Cool Thing", "Check out this cool thing!", "https://github.com/lightningkite/kiteui")
                    }
                }
                button {
                    text("Share image")
                    onClick {
                        val blob = fetch("https://static.wikia.nocookie.net/fzero/images/d/da/Captain_Falcon_SSBU.png").blob()
                        ExternalServices.share(listOf("Captain_Falcon.png" to blob))
                    }
                }
            }

            row {

                button {
                    text { content = "requestFile" }
                    onClick {
                        println(ExternalServices.requestFile(listOf("*/*")))
                    }
                }

                button {
                    text { content = "requestFiles" }
                    onClick {
                        println(ExternalServices.requestFiles(listOf("*/*")))
                    }
                }
            }

            row {
                button {
                    text { content = "requestFile image" }
                    onClick {
                        image.value = ExternalServices.requestFile(listOf("image/*"))?.let { ImageLocal(it) }
                    }
                }

                button {
                    text { content = "requestFiles image" }
                    onClick {
                        image.value =
                            ExternalServices.requestFiles(listOf("image/*"))?.firstOrNull()?.let { ImageLocal(it) }
                    }
                }
            }

            row {
                button {
                    text { content = "requestCaptureSelf" }
                    onClick {
                        image.value = ExternalServices.requestCaptureSelf(listOf("image/*"))?.let { ImageLocal(it) }
                    }
                }

                button {
                    text { content = "requestCaptureEnvironment" }
                    onClick {
                        image.value =
                            ExternalServices.requestCaptureEnvironment(listOf("image/*"))?.let { ImageLocal(it) }
                    }
                }
            }

            sizeConstraints(height = 30.rem) - image {
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
//                    onClick{ExternalServices.setClipboardText(clip.await())}
//                }
//            }
        }
    }
}