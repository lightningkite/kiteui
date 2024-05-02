package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("external-services")
object ExternalServicesScreen : Screen {
    override val title: Readable<String>
        get() = super.title
    val image = Property<ImageSource?>(null)
    override fun ViewWriter.render() {
        col {
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

            button {
                text { content = "requestFile image" }
                onClick {
                    image.value = ExternalServices.requestFile(listOf("image/*"))?.let { ImageLocal(it) }
                }
            }

            button {
                text { content = "requestFiles image" }
                onClick {
                    image.value = ExternalServices.requestFiles(listOf("image/*"))?.firstOrNull()?.let { ImageLocal(it) }
                }
            }

            button {
                text { content = "requestCaptureSelf" }
                onClick {
                    image.value = ExternalServices.requestCaptureSelf(listOf("image/*"))?.let { ImageLocal(it) }
                }
            }

            button {
                text { content = "requestCaptureEnvironment" }
                onClick {
                    image.value = ExternalServices.requestCaptureEnvironment(listOf("image/*"))?.let { ImageLocal(it) }
                }
            }

            expanding - zoomableImage {
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