package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("external-services")
object ExternalServicesScreen : Screen {
    override val title: Readable<String>
        get() = super.title
//    val clip = Property<String>("")
    override fun ViewWriter.render() {
        col {
            col {
                h1 { content = "This screen demonstrates various some external access." }
//                text { content = "Note the use of the multi-layer 'Readable' in `fetching`." }
            } in padded

            button {
                text { content = "openTab" }
                onClick { ExternalServices.openTab("https://google.com") }
            }

            button {
                text { content = "requestFile" }
                onClick { ExternalServices.requestFile(listOf("image/*")) }
            }

            button {
                text { content = "requestFiles" }
                onClick { ExternalServices.requestFiles(listOf("image/*")) }
            }

            button {
                text { content = "requestCaptureSelf" }
                onClick { ExternalServices.requestCaptureSelf(listOf("image/*")) }
            }

            button {
                text { content = "requestCaptureEnvironment" }
                onClick { ExternalServices.requestCaptureEnvironment(listOf("image/*")) }
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