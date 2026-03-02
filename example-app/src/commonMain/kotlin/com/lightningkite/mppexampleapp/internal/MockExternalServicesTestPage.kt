// by Claude - test page for MockExternalServices end-to-end verification
package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*

object MockExternalServicesTestPage : Page {
    override val title: Reactive<String> get() = Constant("Mock External Services Test")

    override fun ViewWriter.render(): Unit = run {
        val fileStatus = Signal("No file")
        val locationResult = Signal("No location")

        scrolling.col {
            h1 { content = "Mock External Services Test" }

            button {
                debugName = "pickFile"
                text { content = "Pick File" }
                onClick {
                    try {
                        val file = externalServices.requestFile(listOf("image/*"))
                        fileStatus.value = file?.fileName() ?: "No file"
                    } catch (e: Exception) {
                        fileStatus.value = "Error: ${e.message}"
                    }
                }
            }

            text {
                debugName = "fileStatus"
                ::content { fileStatus() }
            }

            button {
                debugName = "getLocation"
                text { content = "Get Location" }
                onClick {
                    try {
                        val pos = externalServices.getCurrentPosition()
                        locationResult.value = "${pos.latitude},${pos.longitude}"
                    } catch (e: Exception) {
                        locationResult.value = "Error: ${e.message}"
                    }
                }
            }

            text {
                debugName = "locationResult"
                ::content { locationResult() }
            }

            button {
                debugName = "openLink"
                text { content = "Open Link" }
                onClick {
                    externalServices.openLink("https://example.com")
                }
            }

            button {
                debugName = "setClipboard"
                text { content = "Set Clipboard" }
                onClick {
                    externalServices.setClipboardText("copied")
                }
            }
        }
    }
}
